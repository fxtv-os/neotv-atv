#!/bin/bash

# --- CONFIGURATION ---
# IMPORTANT: DO NOT hardcode your GitHub PAT here.
# Set it as an environment variable: export GITHUB_TOKEN=ghp_...
# Or create a file named .env_token (which is git-ignored) and put it there.
#
# OPTIONAL: For an AI-generated release overview (instead of the free
# plain git-log summary), also set HF_TOKEN (Hugging Face, free tier)
# and/or ANTHROPIC_API_KEY the same way. HF_TOKEN is tried first if set.
# Without either, the script falls back to a free, automatically
# generated summary based on git log/diff.

REPO_URL="github.com/fxtv-os/neotv-atv.git"
BRANCH="main"
PKG_DIR="./packages"

# Load token(s) from local file if it exists
if [ -f ".env_token" ]; then
    source .env_token
fi
# ---------------------

if [ -z "$GITHUB_TOKEN" ]; then
    echo "❌ ERROR: GITHUB_TOKEN is not set."
    echo "Please run: export GITHUB_TOKEN=your_token_here"
    echo "Or create a file '.env_token' with: export GITHUB_TOKEN=your_token_here"
    exit 1
fi

echo "⚡ Starte NeoTV+ All-in-One Build..."

# 1. Java Environment
if [ -z "$JAVA_HOME" ]; then
    if [[ "$OSTYPE" == "darwin"* ]]; then
        export JAVA_HOME=$(/usr/libexec/java_home -v 17) 2>/dev/null
    fi
fi
export JAVA_HOME=${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}
echo "☕ Nutze JAVA_HOME: $JAVA_HOME"

# 2. Extract Version from build.gradle.kts
VERSION=$(grep "versionName =" app/build.gradle.kts | sed 's/.*"\(.*\)".*/\1/')
APP_ID=$(grep "applicationId =" app/build.gradle.kts | sed 's/.*"\(.*\)".*/\1/')

if [ -z "$VERSION" ]; then
    VERSION="unknown"
fi
echo "🏷️  Erkannte Version: $VERSION"

# Build paths
mkdir -p "$PKG_DIR/apk/debug" "$PKG_DIR/apk/release" "$PKG_DIR/aab/debug" "$PKG_DIR/aab/release"

echo "🏗️  Starte Gradle-Build (clean assemble bundle)..."
./gradlew clean assembleRelease bundleRelease assembleDebug bundleDebug --stacktrace

if [ $? -ne 0 ]; then
    echo "❌ Build fehlgeschlagen!"
    exit 1
fi

echo "📦 Archiviere neue Version $VERSION..."

APK_RELEASE="$PKG_DIR/apk/release/${APP_ID}_${VERSION}_release.apk"
AAB_RELEASE="$PKG_DIR/aab/release/${APP_ID}_${VERSION}_release.aab"

# APKs archive
cp app/build/outputs/apk/debug/app-debug.apk "$PKG_DIR/apk/debug/${APP_ID}_${VERSION}_debug.apk" 2>/dev/null
cp app/build/outputs/apk/release/app-release-unsigned.apk "$APK_RELEASE" 2>/dev/null
cp app/build/outputs/apk/release/app-release.apk "$APK_RELEASE" 2>/dev/null

# AABs archive
cp app/build/outputs/bundle/debug/app-debug.aab "$PKG_DIR/aab/debug/${APP_ID}_${VERSION}_debug.aab" 2>/dev/null
cp app/build/outputs/bundle/release/app-release.aab "$AAB_RELEASE" 2>/dev/null

echo "✅ Archivierung abgeschlossen!"

# Git Process
echo "🗂️ Bereite Git-Commit vor..."
if [ ! -d ".git" ]; then
    git init
    git remote add origin "https://$REPO_URL"
    git checkout -b "$BRANCH"
fi

# Vorherigen Tag merken, BEVOR wir den neuen erstellen (für Diff/Übersicht)
PREV_TAG=$(git describe --tags --abbrev=0 2>/dev/null)

git add .

if git diff --cached --quiet; then
    echo "ℹ️ Keine neuen Änderungen gefunden. Push übersprungen."
    exit 0
fi

echo "📤 Pushe Projekt und Builds auf GitHub..."
git commit -m "Build: ${VERSION} ($(date +'%Y-%m-%d %H:%M'))"

PUSH_URL="https://oauth2:${GITHUB_TOKEN}@${REPO_URL}"
git push "$PUSH_URL" "$BRANCH" --force

if [ $? -ne 0 ]; then
    echo "❌ Fehler beim Pushen!"
    exit 1
fi

echo "🎉 Erfolgreich auf GitHub aktualisiert!"

# --- 3. Tag erstellen ---
TAG_NAME="v${VERSION}"

if git rev-parse "$TAG_NAME" >/dev/null 2>&1; then
    echo "ℹ️ Tag $TAG_NAME existiert bereits, überspringe Tag-Erstellung."
else
    git tag -a "$TAG_NAME" -m "Release $TAG_NAME"
    git push "$PUSH_URL" "$TAG_NAME"
    echo "🏷️  Tag $TAG_NAME erstellt und gepusht."
fi

# --- 4. Änderungsübersicht erzeugen (gratis per Default, optional per KI) ---
echo "📝 Erstelle Release-Übersicht..."

if [ -n "$PREV_TAG" ] && [ "$PREV_TAG" != "$TAG_NAME" ]; then
    COMMIT_LOG=$(git log "${PREV_TAG}..${TAG_NAME}" --pretty=format:"- %s (%an)" 2>/dev/null)
    DIFF_STAT=$(git diff "${PREV_TAG}" "${TAG_NAME}" --stat 2>/dev/null | tail -n 20)
else
    COMMIT_LOG=$(git log --pretty=format:"- %s (%an)" -n 30 2>/dev/null)
    DIFF_STAT="(Erster Release – kein Vorgänger-Tag gefunden)"
fi

# Kostenlose Fallback-Übersicht (wird immer erzeugt, genutzt falls kein AI-Key gesetzt ist)
FALLBACK_OVERVIEW="## Änderungen seit ${PREV_TAG:-Projektbeginn}

### Commits
${COMMIT_LOG:-Keine Commit-Historie gefunden.}

### Geänderte Dateien
\`\`\`
${DIFF_STAT}
\`\`\`"

RELEASE_BODY="$FALLBACK_OVERVIEW"

PROMPT_TEXT=$(cat <<EOF
Du bekommst eine Commit-Liste und eine Diff-Statistik zwischen zwei Versionen einer Android-App (NeoTV+). Erstelle daraus eine kurze, gut lesbare Release-Übersicht auf Deutsch in Markdown für GitHub Releases. Gliedere sinnvoll (z.B. Neue Features, Fixes, Sonstiges) falls erkennbar, ansonsten eine einfache Liste. Halte es kompakt (max. ca. 150 Wörter). Gib NUR die fertige Markdown-Übersicht zurück, keine Einleitung, keine Erklärung.

Commits:
${COMMIT_LOG:-Keine Commit-Historie gefunden.}

Geänderte Dateien:
${DIFF_STAT}
EOF
)

if [ -n "$HF_TOKEN" ]; then
    echo "🤖 HF_TOKEN gefunden – erstelle KI-Übersicht über Hugging Face..."

    # Nutzt Hugging Face's OpenAI-kompatible Router-API (kostenloses Kontingent).
    # Modell bei Bedarf anpassen, z.B. auf ein anderes verfügbares Chat-Modell.
    HF_MODEL="${HF_MODEL:-meta-llama/Llama-3.3-70B-Instruct}"

    HF_JSON_PAYLOAD=$(python3 -c '
import json, sys
prompt = sys.stdin.read()
model = sys.argv[1]
print(json.dumps({
    "model": model,
    "max_tokens": 500,
    "messages": [{"role": "user", "content": prompt}]
}))
' "$HF_MODEL" <<< "$PROMPT_TEXT")

    HF_RESPONSE=$(curl -s https://router.huggingface.co/v1/chat/completions \
        -H "Authorization: Bearer ${HF_TOKEN}" \
        -H "Content-Type: application/json" \
        -d "$HF_JSON_PAYLOAD")

    HF_TEXT=$(echo "$HF_RESPONSE" | python3 -c '
import json, sys
try:
    data = json.load(sys.stdin)
    print(data["choices"][0]["message"]["content"].strip())
except Exception:
    pass
')

    if [ -n "$HF_TEXT" ]; then
        RELEASE_BODY="$HF_TEXT"
        echo "✅ KI-Übersicht (Hugging Face) erstellt."
    else
        echo "⚠️  Hugging-Face-Anfrage fehlgeschlagen. Antwort:"
        echo "$HF_RESPONSE"
        echo "ℹ️  Versuche Anthropic als Fallback (falls Key gesetzt) oder nutze automatische Übersicht."
    fi
fi

if [ "$RELEASE_BODY" == "$FALLBACK_OVERVIEW" ] && [ -n "$ANTHROPIC_API_KEY" ]; then
    echo "🤖 ANTHROPIC_API_KEY gefunden – erstelle KI-Übersicht..."

    AI_JSON_PAYLOAD=$(python3 -c '
import json, sys
prompt = sys.stdin.read()
print(json.dumps({
    "model": "claude-haiku-4-5-20251001",
    "max_tokens": 500,
    "messages": [{"role": "user", "content": prompt}]
}))
' <<< "$PROMPT_TEXT")

    AI_RESPONSE=$(curl -s https://api.anthropic.com/v1/messages \
        -H "x-api-key: ${ANTHROPIC_API_KEY}" \
        -H "anthropic-version: 2023-06-01" \
        -H "content-type: application/json" \
        -d "$AI_JSON_PAYLOAD")

    AI_TEXT=$(echo "$AI_RESPONSE" | python3 -c '
import json, sys
try:
    data = json.load(sys.stdin)
    parts = [b["text"] for b in data.get("content", []) if b.get("type") == "text"]
    print("".join(parts).strip())
except Exception:
    pass
')

    if [ -n "$AI_TEXT" ]; then
        RELEASE_BODY="$AI_TEXT"
        echo "✅ KI-Übersicht erstellt."
    else
        echo "⚠️  KI-Anfrage fehlgeschlagen, nutze automatische Übersicht (git log) stattdessen."
    fi
elif [ "$RELEASE_BODY" == "$FALLBACK_OVERVIEW" ]; then
    echo "ℹ️  Kein HF_TOKEN/ANTHROPIC_API_KEY gesetzt – nutze kostenlose, automatische Übersicht (git log/diff)."
fi

# --- 5. GitHub Release erstellen ---
echo "🚀 Erstelle GitHub Release ${TAG_NAME}..."

REPO_PATH=$(echo "$REPO_URL" | sed 's/github.com\///; s/\.git$//')  # z.B. fxtv-os/neotv-atv

RELEASE_JSON=$(python3 -c '
import json, sys
tag = sys.argv[1]
branch = sys.argv[2]
body = sys.argv[3]
print(json.dumps({
    "tag_name": tag,
    "target_commitish": branch,
    "name": tag,
    "body": body,
    "draft": False,
    "prerelease": False
}))
' "$TAG_NAME" "$BRANCH" "$RELEASE_BODY")

RELEASE_RESPONSE=$(curl -s -X POST \
    -H "Authorization: token ${GITHUB_TOKEN}" \
    -H "Accept: application/vnd.github+json" \
    "https://api.github.com/repos/${REPO_PATH}/releases" \
    -d "$RELEASE_JSON")

UPLOAD_URL=$(echo "$RELEASE_RESPONSE" | python3 -c '
import json, sys
try:
    data = json.load(sys.stdin)
    url = data.get("upload_url", "")
    print(url.split("{")[0])
except Exception:
    pass
')

if [ -z "$UPLOAD_URL" ]; then
    echo "❌ Release konnte nicht erstellt werden. Antwort von GitHub:"
    echo "$RELEASE_RESPONSE"
    exit 1
fi

echo "✅ Release ${TAG_NAME} erstellt."

# --- 6. Build-Artefakte an Release anhängen ---
upload_asset() {
    local file_path="$1"
    if [ -f "$file_path" ]; then
        local file_name=$(basename "$file_path")
        echo "📎 Lade ${file_name} hoch..."
        curl -s -X POST \
            -H "Authorization: token ${GITHUB_TOKEN}" \
            -H "Content-Type: application/octet-stream" \
            --data-binary @"$file_path" \
            "${UPLOAD_URL}?name=${file_name}" > /dev/null
    fi
}

upload_asset "$APK_RELEASE"
upload_asset "$AAB_RELEASE"

echo "🎉 Fertig! Release ${TAG_NAME} inkl. Übersicht und Build-Artefakten ist online."