#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
APP_DIR="$PROJECT_DIR/app/src/main"
BS_ANDROID_TOOLCHAIN="${BS_ANDROID_TOOLCHAIN:-$PROJECT_DIR/toolchain}"
BUILD_DIR="$PROJECT_DIR/build"
DIST_DIR="$PROJECT_DIR/dist"
ANDROID_JAR="${ANDROID_JAR:-/usr/lib/android-sdk/platforms/android-23/android.jar}"
if [[ -n "${BS_ANDROID_TOOLCHAIN:-}" ]]; then
  export LD_LIBRARY_PATH="$BS_ANDROID_TOOLCHAIN/usr/lib/x86_64-linux-gnu/android:$BS_ANDROID_TOOLCHAIN/usr/lib/x86_64-linux-gnu:$BS_ANDROID_TOOLCHAIN/usr/lib:${LD_LIBRARY_PATH:-}"
  ANDROID_JAR="$BS_ANDROID_TOOLCHAIN/usr/lib/android-sdk/platforms/android-23/android.jar"
  ecj() { java -Xmx384m -cp "$BS_ANDROID_TOOLCHAIN/usr/share/java/eclipse-jdt-core.jar" org.eclipse.jdt.internal.compiler.batch.Main "$@"; }
  dalvik-exchange() { java -Xmx384m -cp "$BS_ANDROID_TOOLCHAIN/usr/share/java/com.android.dx.jar" com.android.dx.command.Main "$@"; }
  apksigner() { java -Xmx384m -jar "$BS_ANDROID_TOOLCHAIN/usr/share/java/apksigner.jar" "$@"; }
  aapt() { "$BS_ANDROID_TOOLCHAIN/usr/bin/aapt" "$@"; }
  zipalign() { "$BS_ANDROID_TOOLCHAIN/usr/bin/zipalign" "$@"; }
fi
MODE="${1:-debug}"
case "$MODE" in
  debug)
    KEYSTORE="$BUILD_DIR/debug.keystore"
    KEY_ALIAS=androiddebugkey
    export BS_KEYSTORE_PASSWORD=android BS_KEY_PASSWORD=android
    APK="$DIST_DIR/BlackSwordsman3D-v8.0-debug.apk"
    mkdir -p "$BUILD_DIR"
    if [[ ! -f "$KEYSTORE" ]]; then
      keytool -genkeypair -keystore "$KEYSTORE" -alias "$KEY_ALIAS" \
        -storepass android -keypass android -dname 'CN=Android Debug,O=Android,C=US' \
        -keyalg RSA -keysize 2048 -validity 10000 >/dev/null 2>&1
    fi
    ;;
  release)
    if [[ -f "$PROJECT_DIR/.signing.local" ]]; then
      source "$PROJECT_DIR/.signing.local"
    fi
    KEYSTORE="${BS_KEYSTORE:-$PROJECT_DIR/keystore/black-swordsman.keystore}"
    KEY_ALIAS="${BS_KEY_ALIAS:-black_swordsman}"
    : "${BS_KEYSTORE_PASSWORD:?Set BS_KEYSTORE_PASSWORD for release signing}"
    export BS_KEYSTORE_PASSWORD
    export BS_KEY_PASSWORD="${BS_KEY_PASSWORD:-$BS_KEYSTORE_PASSWORD}"
    test -f "$KEYSTORE" || { echo "Signing keystore is missing: $KEYSTORE" >&2; exit 1; }
    APK="$DIST_DIR/BlackSwordsman3D-v8.0.apk"
    ;;
  *) echo "Usage: $0 [debug|release]" >&2; exit 2 ;;
esac

for tool in aapt ecj dalvik-exchange zipalign apksigner; do
  command -v "$tool" >/dev/null 2>&1 || { echo "Missing tool: $tool" >&2; exit 1; }
done

mkdir -p "$BUILD_DIR/classes" "$DIST_DIR"
python3 "$PROJECT_DIR/tools/verify_toolchain.py" "$BS_ANDROID_TOOLCHAIN"
find "$BUILD_DIR/classes" -type f -delete
rm -f "$BUILD_DIR/classes.dex" "$BUILD_DIR/sources.txt" "$BUILD_DIR/unsigned.apk" "$BUILD_DIR/aligned.apk"

rg --files "$APP_DIR/java" -g '*.java' | sort > "$BUILD_DIR/sources.txt"
ecj -proc:none -source 1.7 -target 1.7 -encoding UTF-8 \
  -bootclasspath "$ANDROID_JAR" -d "$BUILD_DIR/classes" @"$BUILD_DIR/sources.txt"

# Bake from current source; generated meshes are never stale after a code change.
mkdir -p "$BUILD_DIR/content"
CONTENT_SOURCES=()
while IFS= read -r name; do CONTENT_SOURCES+=("$APP_DIR/java/com/danil/blackswordsman/$name.java"); done < "$PROJECT_DIR/core-sources.txt"
ecj -proc:none -source 1.7 -target 1.7 -encoding UTF-8 -d "$BUILD_DIR/content" "${CONTENT_SOURCES[@]}" "$PROJECT_DIR/tests/BakeAssets.java"
java -Xmx256m -cp "$BUILD_DIR/content" com.danil.blackswordsman.BakeAssets "$APP_DIR/assets"

dalvik-exchange --dex --min-sdk-version=24 \
  --output="$BUILD_DIR/classes.dex" "$BUILD_DIR/classes"

aapt package -f -0 wav -M "$APP_DIR/AndroidManifest.xml" -S "$APP_DIR/res" \
  -A "$APP_DIR/assets" -I "$ANDROID_JAR" -F "$BUILD_DIR/unsigned.apk"
(cd "$BUILD_DIR" && aapt add unsigned.apk classes.dex >/dev/null)
zipalign -f 4 "$BUILD_DIR/unsigned.apk" "$BUILD_DIR/aligned.apk"

apksigner sign --ks "$KEYSTORE" --ks-key-alias "$KEY_ALIAS" \
  --ks-pass env:BS_KEYSTORE_PASSWORD --key-pass env:BS_KEY_PASSWORD \
  --out "$APK.part" "$BUILD_DIR/aligned.apk"
apksigner verify --verbose "$APK.part"
mv -f "$APK.part" "$APK"
echo "$APK"
