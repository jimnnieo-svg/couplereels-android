# CoupleReels 검증 보고서 — 2026-11-09 (Asia/Seoul)

이 보고서는 P0-1 검증 앱에 대한 것입니다. 전체 MVP 완료 보고서가 아닙니다.

| 항목 | 상태 | 근거/한계 |
|---|---|---|
| 기존 CoupleReels 소스 검색 | 완료 | /workspace 파일·내용 검색에서 발견하지 못함. FashionFit만 확인 |
| Debug APK 빌드 | PASS | Gradle testDebugUnitTest assembleDebug, BUILD SUCCESSFUL |
| Debug APK 서명 | PASS | apksigner v2 signature verifies, signer 1 |
| URL/공유 입력 단위 테스트 | PASS | 6 tests, 0 failures, 0 errors. 추천 알고리즘 테스트 아님 |
| 별도 프로젝트 생성 | PASS | /workspace/CoupleReels, com.couplereels.app |
| 일반 웹 / 공식 임베드 구현 | 구현 완료·실행 미검증 | 직접 URL / blockquote + embed.js 분리 |
| 10개 후보 개별 URL | PASS(형식) | 사용자 제공 URL 그대로 사용. 콘텐츠/태그 검증 아님 |
| 10개 × 2모드 실제 영상 재생 | BLOCKED | 폰/에뮬레이터 연결 없음. playback-matrix.csv의 20개 행 모두 NOT_TESTED |
| 클라우드 Instagram 접속 | BLOCKED | curl HTTPS CONNECT 403 Forbidden, envoy 응답. Instagram 서버의 로그인/재생 실패로 해석하지 않음 |
| 외부 Instagram 앱 실행 여부 | 미검증 | 내비게이션/intent/new window 차단 코드 구현. 설치된 실기기에서 확인 필요 |
| ACTION_SEND 수신 | 구현 완료·실행 미검증 | text/plain URL 추출 및 유효성 검사 |
| 기록/마지막 URL 재실행 유지 | 구현 완료·실행 미검증 | SharedPreferences 사용 |
| WebView 자원 해제 | 코드 검토 완료·실측 미검증 | 화면 해제 destroy, lifecycle onPause/onResume. Profiler 측정 없음 |
| 인터넷/TLS/HTTP 오류 안내 | 구현 완료·실행 미검증 | 메인 프레임 오류, TLS 취소, 재시도 버튼 |
| 인증 차단 안내 | 구현 완료·실행 미검증 | 로그인 경로 감지/수동 결과 분류. 로그인 화면의 모든 변형 감지를 보장하지 않음 |
| Firebase 로그인/동기화 | 미구현 | Firebase 클라이언트 설정 미제공, P0-1 성공 게이트 대기 |
| 커플 연결/다른 커플 데이터 차단 | 미구현 | Firestore 규칙 및 emulator 테스트도 아직 없음 |
| 비공개 원본 숨김/상위10 추천/취향변경 | 미구현 | 추천 엔진 미구현. 기존 PoC의 테스트 6개를 이번 테스트로 주장하지 않음 |
| 폰 2대 교차 추천 시연 | BLOCKED | P0-1 및 Firebase/추천 기능 완료 후 가능 |

## 폰 2대에서 지금 가능한 P0 시연

각 폰에 Probe APK 설치 → 일반 웹 10개 → 공식 임베드 10개 → 각 결과 기록 →
외부 Instagram 앱 이동/로그인 요구/재생 실패/이전·다음/재실행/네트워크 끊김 확인 → JSON 내보내기.
한 폰의 결과가 다른 폰으로 자동 전달되는 기능은 이 버전에 없습니다.

## 사용자 조치가 필요한 조건

1. 설치 가능한 폰에서 실제 재생 결과를 관찰해야 P0-1의 성공 여부를 결정할 수 있습니다.
   클라우드에 기기를 연결할 수 없다면 APK를 내려받아 설치하고 결과 JSON을 전달해야 합니다.
2. 이후 별도 CoupleReels Firebase Android 앱(`com.couplereels.app`)의 google-services.json 제공,
   Email/Password Authentication 및 Firestore 활성화가 필요합니다. 서비스 계정 키는 보내지 마세요.
3. 클라우드에서 Instagram 네트워크 시험까지 하려면 환경 네트워크 설정에서 Instagram 및 실제로 필요한
   콘텐츠 CDN 호스트를 허용해야 합니다. 이것만으로 Android 실기기 재생 검증이 대체되지는 않습니다.

## 범위 결정

사용자는 'P0-1 성공 여부 확인 후 나머지 계속 구현'을 명시했습니다.
재생 여부를 추정해 PASS로 진행하지 않았습니다. 다음 단계는 실제 재생 결과를 받은 후 진행합니다.
이 APK는 추천 피드/인증 화면을 가장하는 비활성 버튼을 포함하지 않습니다.

## 산출물 저장 환경

지정된 `/codex/browser/projectless/0007-couplereels-5-android-mvp-android-firebase-qa-20/output` 경로는
현재 클라우드에서 존재하지 않고 상위 디렉터리 생성 권한도 없습니다. mkdir는 Permission denied, sudo는 설치되어 있지 않았습니다.
대신 `/workspace/output`에 APK·소스 ZIP·검증 보고서를 저장했습니다. 이는 승인 검토 거부가 아닌 실행 환경 파일 권한 제약입니다.
