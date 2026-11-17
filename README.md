# Xboard — source

Keyboard by Hanif. Two parts:

- `android/` — the Xboard keyboard app (Android input method). Plain Java, no Gradle project files.
- `src/`, `public/` — the website that hosts the download page.

## Build the keyboard APK

Requirements: JDK 8+, and Android build tools (aapt2, d8, zipalign, apksigner) plus `android.jar`
for API 34. The script looks for them here:

- `/tmp/android/jdk` — JDK
- `/tmp/android/android-14` — build-tools
- `/tmp/android/android-34/android.jar` — platform jar

Then:

```
bash android/build.sh
```

Output: `android/build/QwertyKeyboard.apk`.

Signing: the script signs with the keystore at `android/keystore/release.jks`
(password `qwerty123`, alias `qwerty`), creating it if missing. Keep that keystore safe —
a different key means the app cannot be installed as an update over an existing install.
Bump `versionCode` in `android/AndroidManifest.xml` for every release.

Tests: `bash android/test.sh` runs the auto-send timing and amount checks.

Install on a phone: copy the APK over, enable "install unknown apps", open it, then in
Android Settings enable Xboard under System > Languages & input > On-screen keyboard and
pick it as the active keyboard.

## Run the website

```
bun install
bun run dev
```

The download page links to `/download/QwertyKeyboard.apk`, so copy the built APK to
`public/download/QwertyKeyboard.apk` before serving the site.

## Keyboard source map

- `QwertyIME.java` — keyboard service: key handling, suggestions, auto-send loop
- `XBar.java` — top bar: Xboard label, Start, Stop, switch keyboard, resize
- `QwertyView.java` — the keys themselves, key previews, touch handling
- `Layouts.java` — letter / symbol / number / phone / emoji layouts
- `Key.java` — key model and codes
- `Theme.java` — the seven color themes
- `Prefs.java` — saved settings (theme, size, wallpaper, message, amount)
- `Wallpaper.java` — custom gallery image behind the keys
- `SuggestionEngine.java` / `SuggestionStrip.java` — word suggestions and the suggestion row
- `SettingsActivity.java` — the Xboard app screen
- `AutoSend.java` — message repeat rules: timing, amount limits
- `res/`, `assets/`, `AndroidManifest.xml` — icons, strings, keyboard declaration, word list
