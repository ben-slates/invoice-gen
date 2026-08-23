#!/usr/bin/env bash
# Build InvoiceGen RELEASE APK with auto-generated signing keystore.
set -euo pipefail

fail() { printf '\nRelease build error: %s\n' "$1" >&2; exit 1; }
root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$root_dir"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$root_dir/.gradle-user-home}"
mkdir -p "$GRADLE_USER_HOME"

# ---------------------------------------------------------------------------
# Determine the Gradle version requested by the wrapper.
# ---------------------------------------------------------------------------
gradle_version="$(sed -nE 's#.*gradle-([0-9]+\.[0-9]+(\.[0-9]+)?)-bin\.zip.*#\1#p' gradle/wrapper/gradle-wrapper.properties | head -n 1)"
[[ -n "$gradle_version" ]] || fail "Could not determine the Gradle version from gradle/wrapper/gradle-wrapper.properties."

# ---------------------------------------------------------------------------
# Select a compatible JDK.
# ---------------------------------------------------------------------------
version_at_least() { [[ "$(printf '%s\n%s\n' "$2" "$1" | sort -V | head -n 1)" == "$2" ]]; }
java_supports_gradle() {
  local major="$1"
  case "$major" in
    17|18|19|20|21|22|23) return 0 ;;
    24) version_at_least "$gradle_version" "8.14" ;;
    25) version_at_least "$gradle_version" "9.1" ;;
    *) return 1 ;;
  esac
}
java_major() { "$1/bin/java" -version 2>&1 | sed -nE '1s/.*"([0-9]+).*/\1/p'; }

JAVA_HOME="${JAVA_HOME:-}"
if [[ -z "$JAVA_HOME" || ! -x "$JAVA_HOME/bin/java" ]]; then
  JAVA_HOME=""
  for candidate in /usr/lib/jvm/java-*-openjdk* /usr/lib/jvm/java-* /usr/java/*; do
    [[ -x "$candidate/bin/java" ]] || continue
    maj="$(java_major "$candidate")"
    java_supports_gradle "$maj" && JAVA_HOME="$candidate" && break
  done
fi
[[ -n "$JAVA_HOME" ]] || fail "No compatible JDK found."
export JAVA_HOME
echo "Java home: $JAVA_HOME"
echo "Java: $("$JAVA_HOME/bin/java" -version 2>&1 | head -1)"
echo "Gradle requested by wrapper: $gradle_version"
echo "Gradle user home: $GRADLE_USER_HOME"

# ---------------------------------------------------------------------------
# Detect Android SDK.
# ---------------------------------------------------------------------------
sdk="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/android/sdk}}"
[[ -d "$sdk" ]] || fail "Android SDK not found at $sdk"
export ANDROID_SDK_ROOT="$sdk"
export ANDROID_HOME="$sdk"
echo "Android SDK: $sdk"

platform="$(ls -d "$sdk/platforms"/android-* 2>/dev/null | sort -t- -k2 -n | tail -1)"
[[ -n "$platform" ]] || fail "No Android platform installed."
echo "Platform: $(basename "$platform")"

build_tools="$(ls -d "$sdk/build-tools"/*/ 2>/dev/null | sort -V | tail -1 | sed 's:/$::')"
[[ -n "$build_tools" ]] || fail "No build tools installed."
echo "Build tools: $(basename "$build_tools")"

# Write local.properties
echo "sdk.dir=$sdk" > "$root_dir/local.properties"

# ---------------------------------------------------------------------------
# Auto-generate signing keystore if it doesn't exist.
# ---------------------------------------------------------------------------
KEYSTORE="$root_dir/release-key.jks"
KEY_ALIAS="invoicegen"
STORE_PASS="invoicegen123"
KEY_PASS="invoicegen123"

if [[ ! -f "$KEYSTORE" ]]; then
    echo ""
    echo "--- Generating release signing keystore ---"
    "$JAVA_HOME/bin/keytool" -genkeypair \
        -keystore "$KEYSTORE" \
        -alias "$KEY_ALIAS" \
        -keyalg RSA \
        -keysize 2048 \
        -validity 10000 \
        -storepass "$STORE_PASS" \
        -keypass "$KEY_PASS" \
        -dname "CN=InvoiceGen, OU=Mobile, O=InvoiceGen, L=Unknown, ST=Unknown, C=PK"
    echo "Keystore created at: $KEYSTORE"
fi

# ---------------------------------------------------------------------------
# Build release APK.
# ---------------------------------------------------------------------------
echo ""
echo "--- Starting Gradle release build ---"
chmod +x "$root_dir/gradlew" 2>/dev/null || true

"$root_dir/gradlew" assembleRelease \
    -Pandroid.injected.signing.store.file="$KEYSTORE" \
    -Pandroid.injected.signing.store.password="$STORE_PASS" \
    -Pandroid.injected.signing.key.alias="$KEY_ALIAS" \
    -Pandroid.injected.signing.key.password="$KEY_PASS" \
    --no-daemon \
    --warning-mode=summary \
    -Dorg.gradle.java.home="$JAVA_HOME"

apk="$root_dir/app/build/outputs/apk/release/app-release.apk"
if [[ -f "$apk" ]]; then
    release_dir="$root_dir/release"
    mkdir -p "$release_dir"
    cp "$apk" "$release_dir/invoice-gen.apk"
    echo ""
    echo "✅ Release APK built successfully!"
    echo "   Source: $apk"
    echo "   Copied: $release_dir/invoice-gen.apk"
    echo "   Size: $(du -h "$release_dir/invoice-gen.apk" | cut -f1)"
else
    fail "Release APK not found at expected location."
fi
