#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"

SDK="${ANDROID_HOME:-$HOME/Android/Sdk}"
[ -d "$SDK" ] || SDK="/opt/android-sdk"

BT=$(ls -d "$SDK/build-tools/"* | sort -V | tail -n 1)
AJ=$(ls "$SDK/platforms/android-"*/android.jar | sort -V | tail -n 1)

rm -rf build && mkdir -p build/gen build/classes build/res
"$BT/aapt2" compile --dir res -o build/res/res.zip
"$BT/aapt2" link -I "$AJ" --manifest AndroidManifest.xml -A assets --java build/gen -o build/base.apk build/res/res.zip --min-sdk-version 23 --target-sdk-version 34
javac -nowarn -Xlint:-options --release 8 -cp "$AJ" -d build/classes $(find src build/gen -name '*.java') 2>&1 | grep -v "^Note:" || true
test -n "$(find build/classes -name QwertyIME.class)"
"$BT/d8" --release --min-api 23 --lib "$AJ" --output build $(find build/classes -name '*.class')
cp build/base.apk build/unsigned.apk
(cd build && zip -q -j unsigned.apk classes.dex)
"$BT/zipalign" -f -p 4 build/unsigned.apk build/aligned.apk
mkdir -p keystore
[ -f keystore/release.jks ] || keytool -genkeypair -keystore keystore/release.jks -storepass qwerty123 -keypass qwerty123 -alias qwerty -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Qwerty Keyboard, O=Qwerty, C=US" >/dev/null 2>&1
"$BT/apksigner" sign --ks keystore/release.jks --ks-pass pass:qwerty123 --ks-key-alias qwerty --out build/Xboard.apk build/aligned.apk
"$BT/apksigner" verify -v build/Xboard.apk | head -5
ls -la build/Xboard.apk
