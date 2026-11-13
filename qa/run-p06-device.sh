#!/usr/bin/env bash
# Dedicated local test device only. Preserves existing data; creates unique QA accounts.
set -euo pipefail
cd "$(dirname "$0")/.."
serial="${1:?Usage: bash qa/run-p06-device.sh SERIAL [OUTPUT]}"
output="${2:-$PWD/build/p06-device}"
mkdir -p "$output"
adb -s "$serial" get-state
[[ "$(adb -s "$serial" shell getprop sys.boot_completed | tr -d '\r')" == 1 ]] || { echo "Device boot is not complete" >&2; exit 1; }
adb -s "$serial" shell getprop ro.build.version.sdk > "$output/device-api.txt"
timeout 180s adb -s "$serial" install -r mvp/build/outputs/apk/debug/mvp-debug.apk > "$output/install.log" 2>&1
timeout 180s adb -s "$serial" install -r mvp/build/outputs/apk/androidTest/debug/mvp-debug-androidTest.apk >> "$output/install.log" 2>&1
timeout 1200s adb -s "$serial" shell am instrument -w -r -e disableAnalytics true -e captureDir p06-captures-verified -e class com.couplereels.app.P06DeviceVerificationTest,com.couplereels.app.P06StartupDeviceTest,com.couplereels.app.P06CoupleDeviceTest,com.couplereels.app.P06UiDeviceTest,com.couplereels.app.P06ProbeDeviceTest com.couplereels.mvp.test/androidx.test.runner.AndroidJUnitRunner > "$output/device-test.log" 2>&1
# am instrument can return shell exit 0 for a crashed or failed runner.
rg -q 'OK \(7 tests\)' "$output/device-test.log"
if rg -q 'FAILURES!!!|Process crashed|INSTRUMENTATION_FAILED' "$output/device-test.log"; then exit 1; fi
adb -s "$serial" pull /sdcard/Android/data/com.couplereels.mvp/files/p06-captures-verified "$output/"
printf '%s\n' 'Native local journey and six regression checks passed. Review real screenshots for overlays/clipping.' 'This does not verify production Firebase, Galaxy S25+ or actual video playback.'
