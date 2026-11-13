# CoupleReels P0-6 검증·발표 준비

P0-5 `70129d1`에서 이어받은 버건디×골드 로컬 MVP입니다. 작성자 기기가 계산한 상위 최대 5개 후보 결과만 전달하고 비공개 태그·원본 해시를 상대에게 보내지 않도록 바꿨습니다.
운영 규칙은 배포하지 않았으며 실제 서버 권한 검증과 Instagram 재생은 별도 확인이 필요합니다.

- [P0-6 검증 결과](docs/P0-6-QA.md)
- [개인정보 계약·운영 적용 조건](docs/P0-6-PRIVACY.md)
- [11월 19일 1~2분 시연·사용자 확인 항목](docs/P0-6-DEMO.md)
- 회귀/빌드: `bash qa/run-p06-regression.sh` (차단 환경의 명시적 JVM 부분집합: `PURE_JVM=1`)
- 실제 기기 검사: 전용 로컬 테스트 기기에서 `bash qa/run-p06-device.sh SERIAL OUTPUT`

아래 P0-5와 P0-3/P0-1 안내는 당시 기록으로 보존합니다.

---

# CoupleReels P0-5 UI integration

P0-4 보존 소스에서 이어받은 Android 로컬 체험 MVP입니다. 4탭 UI·취향 온보딩·Instagram 링크 공유·태그 저장·보관함·커플 교차 추천을 통합했습니다.
운영 Firebase/두 폰 동기화와 실제 Instagram 재생은 미검증이며, 전체 UI/규칙 검사는 네트워크 제한으로 남아 있습니다.

- [P0-5 QA와 시안 7장 반영 범위](docs/P0-5-QA.md)
- [Galaxy S25+ 설치·11월 19일 시연 절차](docs/P0-5-GALAXY-DEMO.md)
- [개인정보 경계·운영 적용 조건](docs/P0-5-SECURITY.md)
- 전체 검증: `bash qa/run-local.sh` (JDK/SDK/Node/npm 의존성 준비 후)
- 기기 UI smoke: 전용 테스트 기기에서 `python qa/device-smoke.py --serial SERIAL --apk APK --output OUTPUT`

기존 앱을 삭제하거나 사용자 데이터를 초기화하지 마세요. 서버 동기화 없이 저장되는 체험 APK를 정식 운영 서비스로 소개하지 않습니다.
아래는 기존 P0-3 및 P0-1 기록입니다.

---

# CoupleReels Android MVP

최신 개발: P0-3 / 2026-11-09. Kotlin + Compose + Firebase.

현재 통합 결과: `docs/P0-3-QA.md`, 실기기 시연: `docs/P0-3-DEVICE-DEMO.md`.
기존29개 포함 총44개 자동 테스트 통과. 실제 Firebase/실기기 재생 검증과 규칙 Emulator 검증은 BLOCKED/미검증입니다.
Firebase 운영 배포 및 외부 업로드는 수행하지 않았습니다.

- `app`: P0-1 WebView 검증 모듈. 기존 소스 변경 없음. `com.couplereels.app`.
- `mvp`: P0-2/3 모듈. `com.couplereels.mvp`. P0-1 화면을 공유 소스로 포함하며 두 APK 동시 설치 가능.
- `firebase`: 보안 규칙, Emulator 테스트. 배포 전 테스트/검토 필요.

JDK 17 이상 및 Android SDK 35를 설치하고 local.properties에 SDK 경로를 지정합니다.

```sh
./gradlew -PprobeOnly=true :app:testDebugUnitTest :app:assembleDebug
./gradlew :mvp:testDebugUnitTest :mvp:assembleDebug
```

APK 위치: `app/build/outputs/apk/debug/app-debug.apk`, `mvp/build/outputs/apk/debug/mvp-debug.apk`.
Firebase 설정 없는 mvp 빌드도 가능하며 로컬 Fake 모드로 시작합니다.
설정이 있으면 Firebase 모드로 시작하며 화면에서 Fake 모드로 전환할 수 있습니다.
Fake 모드는 테스트용이며 폰 간 동기화가 되지 않습니다. 실제 비밀번호를 사용하지 마세요.

P0-2 설정·시연·제한: `docs/MVP.md`, 검증 결과: `docs/MVP-QA.md`.
원래 P0-1 안내와 결과는 아래 및 `docs/QA.md`에 보존합니다.

---

# CoupleReels — P0-1 Android WebView probe

2026-11-09 작성. Kotlin + Jetpack Compose. 별도 패키지 `com.couplereels.app`, min Android 8.0(API 26).

이 버전은 P0-1 기술 검증 APK입니다. 커플 추천 MVP 완성본이 아닙니다.
사용자 지시의 '재생 성공 확인 후 나머지 기능 계속 구현' 게이트 때문에 실기기 재생 확인이 필요합니다.
FashionFit 파일은 수정하지 않았습니다. 기존 CoupleReels/16개 후보 추천 소스는 작업공간에서 발견되지 않았습니다.

## 로컬 빌드

JDK 17 이상(완전한 JDK), Android SDK Platform 35 / Build Tools 35.0.0 필요.
`local.properties`에 본인 SDK 경로를 지정하고 다음을 실행합니다.

```sh
./gradlew testDebugUnitTest assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.
이 환경 전용 프록시/신뢰 저장소 설정은 배포용 프로젝트에 포함하지 않습니다.
기존 서명 앱이 있으면 동일한 debug key가 아닐 경우 업데이트 설치가 실패할 수 있습니다.

## 실기기 설치 및 검증

1. Android 8 이상 폰에서 Debug APK를 설치합니다. 또는 PC에서 `adb install -r app-debug.apk` 실행.
2. 앱을 열면 후보 1/10의 일반 Instagram 웹페이지가 표시됩니다.
3. 영상 재생을 직접 터치합니다. 재생 움직임/소리 확인 전 PASS를 누르지 않습니다.
4. '결과 기록'에서 결과와 메모를 저장합니다. 로그인 요구/재생 실패/네트워크 오류를 구분합니다.
5. 모드 칩을 눌러 공식 임베드로 전환하고 동일하게 확인·기록합니다.
6. '다음'으로 10개 URL 각각 반복합니다. 총 20개 조합이 필요합니다.
7. Instagram 앱이 폰에 설치된 상태에서도 외부 실행 여부를 확인합니다. 차단 안내가 나면 실제 외부 실행과 구분해 메모합니다.
8. 앱을 종료하고 다시 열어 마지막 URL과 결과 기록이 유지되는지 확인합니다.
9. 일반/임베드 전환, 이전/다음을 30회 반복하고 회전·백그라운드 전환 후 오류를 확인합니다.
10. 비행기 모드에서 '열기/재시도'로 오류 안내를 확인하고 네트워크 복구 후 재시도합니다.
11. Instagram의 공유 → CoupleReels Probe로 ACTION_SEND URL 수신을 확인합니다.
12. '기록 내보내기'로 JSON을 전달합니다. 내보내기는 사용자가 직접 선택한 공유 앱을 실행합니다.

페이지 로드 완료/HTTP 200은 영상 재생 성공의 증거가 아닙니다. 기록은 수동 관찰값입니다.
메모에 계정 비밀번호/인증코드 등 비밀정보를 입력하지 마세요.

## 보안과 제한

- 입력: HTTPS + 정확한 instagram.com/www.instagram.com 호스트 + 개별 `/reel/{id}/` 경로만 허용. 추적 쿼리는 제거.
- 내비게이션: Instagram HTTPS만 유지. intent/custom scheme/외부 브라우저 이동, 다운로드, 새 창을 허용하지 않습니다.
- JavaScript, DOM Storage 활성화. 사용자 터치 후 미디어 재생. 파일/콘텐츠 접근, 혼합 콘텐츠, 위험한 JS 브리지 없음.
- TLS 오류는 취소. 웹 카메라/마이크 권한 요청 거부. Safe Browsing 사용.
- 제3자 쿠키 차단. 이 설정으로 임베드 로그인/재생이 제한될 수 있으며 우회하지 않습니다.
- 공식 임베드: Instagram 공식 blockquote와 embed.js를 사용. 일반 웹: 원본 Reel URL을 직접 로드.
- Instagram 인증·콘텐츠 제한을 우회하지 않으며 스크래핑/다운로드/재업로드하지 않습니다.
- 화면 해제 시 stopLoading/removeAllViews/destroy, lifecycle pause/resume 처리. 실제 메모리 누수 검증은 미완료.
- 전체화면 재생/가로 모드 최적화/자동 스와이프는 구현하지 않았습니다. 이전·다음 버튼으로 전환합니다.
- JSON은 앱 로컬에 저장됩니다. 제거/앱 데이터 삭제 시 사라집니다. Firebase 공유가 아닙니다.

## 다음 게이트

실기기에서 적어도 한 URL/모드의 재생을 확인하고 모드별 10개 매트릭스를 채운 뒤 다음 개발 단계로 진행합니다.
Firebase에는 별도 CoupleReels 프로젝트의 Android 앱 등록(package `com.couplereels.app`),
Email/Password Authentication 활성화 및 Firestore 생성이 필요합니다.
`google-services.json`은 Android 클라이언트 설정이며 서비스 계정 키는 필요하지 않습니다.
기존 FashionFit Firebase 설정을 재사용하지 않습니다.

아직 구현하지 않은 것: Firebase 인증/연결/규칙, 커플 연결·해제, 취향 저장, 파트너 실시간 피드,
추천 알고리즘, 저장·관심 표시, 두 사용자 동기화, 다른 커플 접근 차단 테스트.
