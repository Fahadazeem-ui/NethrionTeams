#!/bin/sh
set -eu
GRADLE_VERSION="9.7.0"
DIST_DIR="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/dists/gradle-${GRADLE_VERSION}-bin"
ZIP="$DIST_DIR/gradle-${GRADLE_VERSION}-bin.zip"
INSTALL="$DIST_DIR/gradle-${GRADLE_VERSION}"
if [ -x "$INSTALL/bin/gradle" ]; then
  exec "$INSTALL/bin/gradle" "$@"
fi
mkdir -p "$DIST_DIR"
TMP="$DIST_DIR/.gradle.zip.tmp"
URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
printf '%s\n' "Gradle ${GRADLE_VERSION} not found. Downloading..."
curl -fL --retry 3 --retry-delay 2 -o "$TMP" "$URL"
rm -rf "$INSTALL"
unzip -q "$TMP" -d "$DIST_DIR"
rm -f "$TMP"
exec "$INSTALL/bin/gradle" "$@"
