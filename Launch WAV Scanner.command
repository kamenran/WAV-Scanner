#!/usr/bin/env bash
set -euo pipefail

APP_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR_PATH="$APP_DIR/dist/WAV-Scanner.jar"

if ! command -v java >/dev/null 2>&1; then
  osascript -e 'display dialog "Java is required to run WAV Scanner. Install Java 17 or newer, then try again." buttons {"OK"} default button "OK" with icon caution' >/dev/null 2>&1 || true
  echo "Java is required to run WAV Scanner. Install Java 17 or newer, then try again."
  exit 1
fi

if [ ! -f "$JAR_PATH" ]; then
  if [ -x "$APP_DIR/build.sh" ]; then
    "$APP_DIR/build.sh"
  else
    echo "Could not find dist/WAV-Scanner.jar or build.sh."
    exit 1
  fi
fi

cd "$APP_DIR"
java -jar "$JAR_PATH"
