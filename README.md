# NeoTV-ATV

**NeoTV** ist ein nativer Android TV Launcher für IPTV- und App-basierte TV-Navigation. Das Projekt bietet eine schlanke Leanback-Oberfläche, einen Fokus auf Live-TV und direkte App-Starts, ohne WebView oder webOS-Abhängigkeiten.

## Was steckt drin?

- Native Android TV App mit Kotlin + AndroidX Leanback
- Live TV aus lokalen M3U-Assets (`basis_sender.m3u`, `fasttv.m3u`, `custom.m3u`)
- Apps-Startseiten mit direktem App-Launch oder Play Store Fallback
- Deep-Link-„Channels aus Apps“ für den direkten Einstieg in andere Apps
- App-interne Sprachumschaltung (DE / EN / NL / TR)
- Kein Benutzer-Upload externer M3U-URLs – alle Listen stammen aus den App-Assets

## Highlights

- **Plug-and-play IPTV**: Alle `.m3u`-/`.m3u8`-Dateien aus `app/src/main/assets/m3u/` werden automatisch geladen und zusammengeführt.
- **Kein WebView**: echte native Android-TV-UI mit Leanback-Browse-Screen und Fokusnavigation.
- **App-internes Sprachsystem**: Spracheinstellungen unabhängig vom System, gespeichert in SharedPreferences.
- **Erweiterbarer Launcher-Inhalt**: Mehr Reihen, zusätzliche App-Links und eigene M3U-Dateien einfach hinzufügen.

## Projektstruktur

- `androidtv_generic_v2/` – Hauptprojekt mit Launcher-App
- `androidtv_launcher_generic/` – alternative Launcher-Struktur / Basisprojekt

### Wichtige Pfade in `androidtv_generic_v2`

- `app/src/main/assets/m3u/` – M3U-Listen zur Build-Zeit
- `app/src/main/assets/apps_channels.json` – App-Kacheln und App-interne Channel-Links
- `app/src/main/java/.../ui/` – Leanback UI, Suche, Playback, Einstellungen
- `app/src/main/java/.../data/` – Parser, Repositorys, Asset-Loader
- `app/src/main/java/.../util/` – LocaleHelper, BaseActivity, allgemeine Helfer

## Was funktioniert bereits?

- Benutzer navigiert per D-Pad durch einen Leanback-Launcher
- Live-TV-Kanäle aus lokalen M3U-Dateien werden geladen und angezeigt
- Streaming-Apps können direkt gestartet werden oder zeigen bei fehlender App den Play Store
- In-App-Sprache kann vom Nutzer gewählt werden
- Eigene `custom.m3u` unterstützt benutzerdefinierte Senderlisten

## Schnellstart

```bash
cd androidtv_generic_v2
./gradlew assembleDebug
```

Die APK findet sich danach unter:

```bash
androidtv_generic_v2/app/build/outputs/apk/debug/app-debug.apk
```

### Empfohlenes Testen

- Verwende einen Android-TV-Emulator oder ein echtes Android-TV-Gerät
- Für Fokus-Navigation teste mit D-Pad / Pfeiltasten
- ADB-Install:

```bash
adb install -r androidtv_generic_v2/app/build/outputs/apk/debug/app-debug.apk
```

## Anpassung und Erweiterung

- `app/src/main/assets/m3u/custom.m3u` ersetzen, um eigene Senderlisten einzubinden
- Weitere `.m3u`-Dateien in `app/src/main/assets/m3u/` hinzufügen, um Content zu erweitern
- `apps_channels.json` anpassen, um App-Direktlinks oder zusätzliche Kanalziele zu definieren
- Weitere Sprachen hinzufügen über neue `values-xx/strings.xml`-Resourcen

## Was du wissen solltest

- Das Projekt lädt nur lokale Asset-Listen, keine externe M3U-URL-Eingabe durch Nutzer
- Deep-Links zu Apps sind implementierbar, aber Anbieter-spezifisch; überprüfe die aktuellen URI-Schemata
- Für Release-Builds ist eine eigene Signatur/Keystore-Konfiguration nötig

## Vorschläge für nächste Schritte

- EPG-/Programmführer-Integration (XMLTV)
- Favoriten und Kanal-Filter
- Stream-Qualitätsauswahl oder Player-Optionen
- Benutzerdefinierte Startseite / eigene Reihen

## Kontakt

Bei Fragen oder Wunsch nach Feature-Änderungen einfach direkt im Repo kommentieren oder ein Issue erstellen.
