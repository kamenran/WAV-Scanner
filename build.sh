#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="$ROOT_DIR/build"
CLASSES_DIR="$BUILD_DIR/classes"
DIST_DIR="$ROOT_DIR/dist"
PACKAGES_DIR="$DIST_DIR/packages"

rm -rf "$BUILD_DIR" "$DIST_DIR"
mkdir -p "$CLASSES_DIR" "$DIST_DIR" "$PACKAGES_DIR"

javac -cp "$ROOT_DIR/libs/*" -d "$CLASSES_DIR" "$ROOT_DIR/src/WAVKeyDetector.java"

for jar_file in "$ROOT_DIR"/libs/*.jar; do
  (cd "$CLASSES_DIR" && jar xf "$jar_file")
done

find "$CLASSES_DIR" -name "module-info.class" -delete

jar --create \
  --file "$DIST_DIR/WAV-Scanner.jar" \
  --main-class WAVKeyDetector \
  -C "$CLASSES_DIR" .

cat > "$DIST_DIR/Launch WAV Scanner.command" <<'LAUNCHER'
#!/usr/bin/env bash
set -euo pipefail

APP_DIR="$(cd "$(dirname "$0")" && pwd)"

if ! command -v java >/dev/null 2>&1; then
  osascript -e 'display dialog "Java is required to run WAV Scanner. Install Java 17 or newer, then try again." buttons {"OK"} default button "OK" with icon caution' >/dev/null 2>&1 || true
  echo "Java is required to run WAV Scanner. Install Java 17 or newer, then try again."
  exit 1
fi

cd "$APP_DIR"
java -jar "WAV-Scanner.jar"
LAUNCHER

cat > "$DIST_DIR/Launch WAV Scanner.bat" <<'LAUNCHER'
@echo off
setlocal

where java >nul 2>nul
if errorlevel 1 (
  echo Java is required to run WAV Scanner.
  echo Install Java 17 or newer, then try again.
  pause
  exit /b 1
)

cd /d "%~dp0"
java -jar "WAV-Scanner.jar"
if errorlevel 1 pause
LAUNCHER

chmod +x "$DIST_DIR/Launch WAV Scanner.command"

MAC_APP_DIR="$BUILD_DIR/package-mac/WAV Scanner"
WINDOWS_APP_DIR="$BUILD_DIR/package-windows/WAV Scanner"
mkdir -p "$MAC_APP_DIR" "$WINDOWS_APP_DIR"

cp "$DIST_DIR/WAV-Scanner.jar" "$MAC_APP_DIR/"
cp "$DIST_DIR/Launch WAV Scanner.command" "$MAC_APP_DIR/"
cat > "$MAC_APP_DIR/README.txt" <<'README'
WAV Scanner for macOS

Double-click "Launch WAV Scanner.command" to open the app.

Requires Java 17 or newer.
README

cp "$DIST_DIR/WAV-Scanner.jar" "$WINDOWS_APP_DIR/"
cp "$DIST_DIR/Launch WAV Scanner.bat" "$WINDOWS_APP_DIR/"
cat > "$WINDOWS_APP_DIR/README.txt" <<'README'
WAV Scanner for Windows

Double-click "Launch WAV Scanner.bat" to open the app.

Requires Java 17 or newer.
README

(cd "$BUILD_DIR/package-mac" && zip -qr "$PACKAGES_DIR/WAV-Scanner-mac.zip" "WAV Scanner")
(cd "$BUILD_DIR/package-windows" && zip -qr "$PACKAGES_DIR/WAV-Scanner-windows.zip" "WAV Scanner")

echo "Built dist/WAV-Scanner.jar"
echo "On macOS, double-click dist/Launch WAV Scanner.command"
echo "On Windows, double-click dist/Launch WAV Scanner.bat"
echo "Download zips:"
echo "- dist/packages/WAV-Scanner-mac.zip"
echo "- dist/packages/WAV-Scanner-windows.zip"
