#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
gradle_bin="${GRADLE_BIN:-./gradlew}"
output="${QA_OUTPUT:-$PWD/build/p06-delivery}"
mkdir -p "$output"
if [[ "${PURE_JVM:-0}" == 1 ]]; then
 "$gradle_bin" -I qa/pure-jvm.init.gradle --no-daemon --max-workers=2 :app:testDebugUnitTest :mvp:testDebugUnitTest --tests '*ReelUrlTest' --tests '*MvpTest' --tests '*CrossRecommendationTest' --tests '*P05RegressionTest' --tests '*PrivateProjectionTest' :app:lintDebug :mvp:lintDebug :app:assembleDebug :mvp:assembleDebug :mvp:assembleDebugAndroidTest 2>&1 | tee "$output/regression.log"
else
 "$gradle_bin" --no-daemon --max-workers=2 :app:testDebugUnitTest :mvp:testDebugUnitTest :app:lintDebug :mvp:lintDebug :app:assembleDebug :mvp:assembleDebug :mvp:assembleDebugAndroidTest 2>&1 | tee "$output/regression.log"
fi
(cd firebase && npm run test:gate) 2>&1 | tee "$output/firebase-gate.log"
cp mvp/build/outputs/apk/debug/mvp-debug.apk "$output/CoupleReels-P0-6-debug.apk"
cp mvp/build/outputs/apk/androidTest/debug/mvp-debug-androidTest.apk "$output/CoupleReels-P0-6-tests.apk"
(cd "$output" && sha256sum *.apk > SHA256SUMS)
printf '%s\n' 'Firestore and native device checks are separate. Partial JVM mode does not validate Robolectric UI tests.'
