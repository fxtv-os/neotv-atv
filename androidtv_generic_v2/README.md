# NeoTV – Android TV Launcher (Kotlin / Leanback)

Native Android-TV-App (kein WebView, kein webOS mehr), gebaut mit Kotlin +
AndroidX Leanback (das "alte" Sidebar-Design wie bei VLC für Android TV).

## Kategorien (Reihen im Hauptmenü)

1. **Suche** – eigene Zeile mit zwei Kacheln: "Suche starten" (Leanback-Suche
   über Sender **und** Apps) und "Google Assistant" (ruft `ACTION_VOICE_COMMAND`
   auf, öffnet den auf dem Gerät installierten Assistant).
2. **Live TV** – eine Zeile pro `group-title` aus den gebündelten M3U-Dateien.
3. **Apps** – Kacheln für Streaming-Apps (Disney+, Netflix, Prime Video,
   YouTube, Spotify …), öffnet die App bzw. leitet bei Nichtinstallation zum
   Play-Store-Eintrag weiter.
4. **Channels aus Apps** – einzelne "Sender"/Deep-Links *in* andere Apps
   hinein (z. B. direkt Disney+ öffnen). Konfiguriert in
   `app/src/main/assets/apps_channels.json`.
5. **Einstellungen** – aktuell nur Sprachwahl (DE/EN/NL/TR), leicht erweiterbar.

## Deine Anforderungen A–D

- **A – Mehrere M3Us**: Es liegen `basis_sender.m3u`, `fasttv.m3u` und
  `custom.m3u` unter `app/src/main/assets/m3u/`. `ChannelRepository` lädt
  **automatisch alle** `.m3u`/`.m3u8`-Dateien in diesem Ordner und merged sie.
  Willst du eine weitere Liste hinzufügen: einfach neue `.m3u`-Datei in den
  Ordner legen und App neu bauen.
- **B – Keine Wechsel-Möglichkeit für den Nutzer**: Es gibt bewusst **keine**
  UI zum Eintragen/Ändern einer externen M3U-URL. Listen kommen ausschließlich
  aus den Asset-Dateien, die zur Build-Zeit ins APK gepackt werden.
- **C – Sprachwechsel**: DE, EN, NL, TR über Einstellungen → Sprache. Das ist
  eine **App-eigene** Sprache (unabhängig vom Android-TV-Systemlauf), gespeichert
  in SharedPreferences und über `LocaleHelper`/`BaseActivity` global angewendet.
- **D – Eigene M3U**: `custom.m3u` ist als Platzhalter für deine
  selbst zusammengebaute Liste gedacht – einfach den Inhalt ersetzen.

## Build & Test nur mit GitHub Codespace + Online-Emulator

Da Android Studio hier nicht zur Verfügung steht, läuft alles über die
Kommandozeile (Gradle) im Codespace:

```bash
# 1. Repo/Projekt in den Codespace pushen bzw. dort entpacken
unzip NeoTV-AndroidTV.zip && cd NeoTV-AndroidTV

# 2. Gradle-Wrapper einmalig erzeugen (Codespace hat Internetzugang)
gradle wrapper --gradle-version 8.7
#   Falls "gradle" nicht vorhanden ist:
#   sudo apt-get update && sudo apt-get install -y openjdk-17-jdk unzip
#   dann: curl -s "https://get.sdkman.io" | bash && sdk install gradle 8.7

# 3. Android SDK Kommandozeilen-Tools installieren (einmalig)
mkdir -p ~/android-sdk/cmdline-tools
cd ~/android-sdk/cmdline-tools
curl -o cmdline-tools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip cmdline-tools.zip && mv cmdline-tools latest
export ANDROID_SDK_ROOT=~/android-sdk
export PATH=$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# 4. Zurück ins Projekt, Debug-APK bauen
cd -   # zurück zu NeoTV-AndroidTV
export ANDROID_SDK_ROOT=~/android-sdk
./gradlew assembleDebug
```

Die fertige APK liegt danach unter:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Auf dem Online-Emulator testen

Die meisten Online-Android-Emulatoren (z. B. Appetize.io) akzeptieren eine
hochgeladene `.apk` direkt über die Weboberfläche – lade dort einfach
`app-debug.apk` hoch. Alternativ, falls dein Emulator ADB über ein
Web-Terminal erlaubt:

```bash
adb connect <emulator-host>:<port>
adb install app/build/outputs/apk/debug/app-debug.apk
```

> Hinweis: Manche Cloud-Emulatoren emulieren ein normales Handy-Layout, kein
> "echtes" Android-TV-Gerät. Für den echten Leanback-Look (10-Foot-UI, D-Pad-
> Fokus-Navigation) ist ein Android-TV-Systemimage / echtes TV-Gerät am
> genauesten. Auf einem Telefon-Emulator lässt sich die App trotzdem starten
> und die Fokus-Navigation mit Pfeiltasten/Tab testen.

## Wichtige Erweiterungspunkte

- **Echte Deep-Links in Apps** (`Channels aus Apps`): Die konkreten Deep-Link-
  Schemata unterscheiden sich je Anbieter und ändern sich gelegentlich. Die
  Einträge in `apps_channels.json` sind Platzhalter – für Disney+/Netflix/etc.
  musst du die jeweils aktuellen, offiziell unterstützten Deep-Link-URIs
  einsetzen (sofern der Anbieter das erlaubt).
- **Weitere Sprachen**: neuen `values-xx/strings.xml`-Ordner anlegen und in
  `arrays.xml` (`language_values`) ergänzen.
- **EPG/Timeshift**: aktuell reiner Live-Stream-Player (Media3/ExoPlayer,
  HLS-fähig). Für ein EPG-Band bräuchte es eine zusätzliche Datenquelle (XMLTV).
- **Signierung für Release**: Für eine echte Veröffentlichung/Sideload auf
  Dauer solltest du einen Keystore anlegen und `assembleRelease` statt
  `assembleDebug` verwenden.

## Projektstruktur

```
app/src/main/java/com/neoos/neotv/
  ui/main/        MainActivity, MainFragment (Leanback Browse), CardPresenter
  ui/search/      SearchActivity, SearchFragment (Leanback Search)
  ui/playback/    PlaybackActivity (Media3 ExoPlayer, HLS)
  ui/settings/    SettingsActivity, SettingsFragment (Sprachwahl)
  data/           M3uParser, ChannelRepository, AppsRepository
  model/          Channel, AppTile, AppChannel
  util/           LocaleHelper, BaseActivity
app/src/main/assets/
  m3u/            *.m3u – deine gebündelten Senderlisten (A, B, D)
  apps_channels.json  Konfiguration für Apps-Kacheln + App-interne Channels
```
