#!/usr/bin/env bash

set -euo pipefail

PACKAGE_NAME="com.lengyuefenghua.newsreader"
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

fail() {
    printf 'ERROR: %s\n' "$1" >&2
    exit 1
}

step() {
    printf '==> %s\n' "$1"
}

if ! command -v adb >/dev/null 2>&1; then
    fail "adb not found in PATH"
fi

step "Find device"
device_serial="$({ adb devices | awk 'NR > 1 && $2 == "device" { print $1; exit }'; } || true)"

if [[ -z "$device_serial" ]]; then
    fail "No available device found"
fi

step "Using device: $device_serial"

step "Build and install debug apk"
(
    cd "$SCRIPT_DIR"
    ./gradlew installDebug
)

step "Force stop old process"
adb -s "$device_serial" shell am force-stop "$PACKAGE_NAME"

step "Launch app via launcher"
adb -s "$device_serial" shell monkey -p "$PACKAGE_NAME" -c android.intent.category.LAUNCHER 1

step "Done"
