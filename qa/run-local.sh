#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
gradle_bin="${GRADLE_BIN:-./gradlew}"
qa_output="${QA_OUTPUT:-$PWD/build/p05-delivery}"
mkdir -p "$qa_output"
if [[ "${PURE_JVM:-0}" == 1 ]]; then
  # This is a subset, not a successful full UI test run.
  "$gradle_bin" -I qa/pure-jvm.init.gradle --no-daemon --max-workers=3 :app:testDebugUnitTest :mvp:testDebugUnitTest --tests '*ReelUrlTest' --tests '*MvpTest' --tests '*CrossRecommendationTest' --tests '*P05RegressionTest' :app:lintDebug :mvp:lintDebug :app:assembleDebug :mvp:assembleDebug 2>&1 | tee "$qa_output/gradle-pure.log"
else
  "$gradle_bin" --no-daemon --max-workers=3 :app:testDebugUnitTest :mvp:testDebugUnitTest :app:lintDebug :mvp:lintDebug :app:assembleDebug :mvp:assembleDebug 2>&1 | tee "$qa_output/gradle-full.log"
fi
(cd firebase && npm run test:gate) 2>&1 | tee "$qa_output/firebase-gate.log"
cp mvp/build/outputs/apk/debug/mvp-debug.apk "$qa_output/CoupleReels-P0-5-debug.apk"
cp app/build/outputs/apk/debug/app-debug.apk "$qa_output/CoupleReels-P0-1-probe-debug.apk"
(cd "$qa_output" && sha256sum ./*.apk > SHA256SUMS)
printf '%s\n' "Artifacts: $qa_output" "Firestore Emulator, production Firebase and physical-device playback require separate validation."
