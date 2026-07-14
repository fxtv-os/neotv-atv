#!/bin/bash

# --- CONFIGURATION ---
# IMPORTANT: DO NOT hardcode your GitHub PAT here.
# Set it as an environment variable: export GITHUB_TOKEN=ghp_...
# Or create a file named .env_token (which is git-ignored) and put it there.

REPO_URL="github.com/fxtv-os/neotv-atv.git"
BRANCH="main"
PKG_DIR="./packages"

# Load token from local file if it exists
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

if [ $? -eq 0 ]; then
    echo "📦 Archiviere neue Version $VERSION..."
    
    # APKs archive
    cp app/build/outputs/apk/debug/app-debug.apk "$PKG_DIR/apk/debug/${APP_ID}_${VERSION}_debug.apk" 2>/dev/null
    cp app/build/outputs/apk/release/app-release-unsigned.apk "$PKG_DIR/apk/release/${APP_ID}_${VERSION}_release.apk" 2>/dev/null
    cp app/build/outputs/apk/release/app-release.apk "$PKG_DIR/apk/release/${APP_ID}_${VERSION}_release.apk" 2>/dev/null
    
    # AABs archive
    cp app/build/outputs/bundle/debug/app-debug.aab "$PKG_DIR/aab/debug/${APP_ID}_${VERSION}_debug.aab" 2>/dev/null
    cp app/build/outputs/bundle/release/app-release.aab "$PKG_DIR/aab/release/${APP_ID}_${VERSION}_release.aab" 2>/dev/null
    
    echo "✅ Archivierung abgeschlossen!"
    
    # Git Process
    echo "🗂️ Bereite Git-Commit vor..."
    if [ ! -d ".git" ]; then
        git init
        git remote add origin "https://$REPO_URL"
        git checkout -b "$BRANCH"
    fi

    git add .

    if git diff --cached --quiet; then
        echo "ℹ️ Keine neuen Änderungen gefunden. Push übersprungen."
    else
        echo "📤 Pushe Projekt und Builds auf GitHub..."
        git commit -m "Build: ${VERSION} ($(date +'%Y-%m-%d %H:%M'))"

        PUSH_URL="https://oauth2:${GITHUB_TOKEN}@${REPO_URL}"
        git push "$PUSH_URL" "$BRANCH" --force
        
        if [ $? -eq 0 ]; then
            echo "🎉 Erfolgreich auf GitHub aktualisiert!"
        else
            echo "❌ Fehler beim Pushen!"
        fi
    fi
else
    echo "❌ Build fehlgeschlagen!"
    exit 1
fi
