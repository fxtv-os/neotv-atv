#!/bin/bash

# 1. Umgebung einrichten
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
PKG_DIR="./packages"

# Zielordner erstellen
mkdir -p "$PKG_DIR/apk/debug" "$PKG_DIR/apk/release" "$PKG_DIR/aab/debug" "$PKG_DIR/aab/release"

echo "⚡ Starte deinen All-in-One-Build..."

# Exakt dein Befehl, der nachweislich funktioniert
./gradlew assembleRelease bundleRelease assembleDebug bundleDebug --stacktrace

# Erst wenn Gradle zu 100% fertig ist, fassen wir die Dateien an!
if [ $? -eq 0 ]; then
    echo "📦 Kopiere fertig umbenannte Dateien in die Git-Struktur..."
    
    # APKs kopieren
    cp app/build/outputs/apk/debug/*.apk "$PKG_DIR/apk/debug/" 2>/dev/null
    cp app/build/outputs/apk/release/*.apk "$PKG_DIR/apk/release/" 2>/dev/null
    
    # AABs kopieren
    cp app/build/outputs/bundle/debug/*.aab "$PKG_DIR/aab/debug/" 2>/dev/null
    cp app/build/outputs/bundle/release/*.aab "$PKG_DIR/aab/release/" 2>/dev/null
    
    echo "✅ Sortierung abgeschlossen!"
    
    # Git Push-Prozess
    echo "🗂️ Bereite Git-Commit vor..."
    git add packages/
    
    if git diff-index --quiet HEAD -- packages/; then
        echo "ℹ️ Keine neuen Änderungen in den Packages gefunden. Push übersprungen."
    else
        echo "📤 Pushe neue Builds auf den GitHub main branch..."
        git commit -m "Build: Neue APKs und AABs automatisch generiert"
        git push origin main
        
        if [ $? -eq 0 ]; then
            echo "🎉 Erfolgreich auf GitHub gepusht! Deine Dateien sind online."
        else
            echo "❌ Fehler beim Pushen auf GitHub!"
        fi
    fi
else
    echo "❌ Fehler beim Build! Git-Push abgebrochen."
    exit 1
fi
