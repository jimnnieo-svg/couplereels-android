# CoupleReels P0-2/3 검증 보고서

2026-11-09 (Asia/Seoul). 실제 소스 개발·빌드·테스트 결과입니다.

| 항목 | 결과 | 근거 / 남은 한계 |
|---|---|---|
| P0-1 소스 보존 | PASS | 원본 ZIP의 app 모듈 파일 6개와 바이트 동일 |
| P0-1 독립 빌드 | PASS | -PprobeOnly=true :app:testDebugUnitTest :app:assembleDebug 성공 |
| P0-1 APK 보존 | PASS | 재빌드 APK와 원본 SHA-256 동일 |
| P0-2/3 APK 빌드 | PASS | :mvp:testDebugUnitTest :mvp:assembleDebug BUILD SUCCESSFUL |
| 새 APK 서명 검증 | PASS | apksigner v2 verifies, signer 1 |
| Reel URL 보안/공유 입력 | PASS | JVM 단위 테스트 6개 |
| 추천 엔진 | PASS | JVM 테스트 7개: 가중유사도, 태그변경, 중복제거, 비공개원본제외, 10개제한/URL검증, 미분류태그, 동점결정성 |
| Fake 계정/커플 흐름 | PASS | JVM 테스트 9개: 로그인/프로필, 인증실패, 두계정태그전달, 3인거부, 해제, 개인저장, 복원, 커플분리, 저장실패 |
| 네트워크·인증 오류 안내 | PASS | Robolectric 테스트 6개: 네트워크, 자격증명, 권한, 세션만료, 진단정보 숨김, 입력안내 |
| Firebase 설정 없는 앱 시작 | PASS | Robolectric Android API35 시작·종료 테스트 1개. 실기기 검증 아님 |
| Firebase 실제 구현 클래스 | 컴파일 PASS / 실제 연결 미검증 | Auth SDK, Firestore listener와 transaction 구현. 클라이언트 설정 미제공 |
| 실제 이메일/비밀번호 인증 | 미검증 | Fake 테스트와 실제 Auth 서비스를 구분 |
| 실제 폰 두 대 취향 동기화 | 미검증 | Firestore 설정·폰 필요. Fake 공유Backend 테스트는 폰 간 통신이 아님 |
| Firestore 보안 규칙 | 작성 완료 / 실행 BLOCKED | 10개 보안 테스트 작성. 공식 emulator JAR 다운로드가 403 Domain forbidden |
| 다른 커플 실제 서버 접근 차단 | 미검증 | Emulator/실제 서버 테스트 통과를 주장하지 않음 |
| 재로그인·데이터 유지 | Fake 모델 복원 PASS / 실기기 미검증 | JSON roundtrip+session 복원 검증. Firebase Auth SDK 지속성은 실제 재실행 확인 필요 |
| P0-1 10 URL × 2모드 영상 재생 | 미검증 | 기존 미검증 상태 유지. 기존 playback-matrix.csv 사용 |
| 실제 WebView 메모리 누수/터치/외부 앱 이동 | 미검증 | 실기기 관찰 필요 |
| 다운로드 첨부 제공 | BLOCKED | 현재 도구에 로컬 파일 → 채팅 첨부/아티팩트 업로드 API 없음. /workspace는 다운로드 URL이 아님 |

MVP 테스트 합계 29개(추천/Fake 16 + URL 6 + 시작 1 + 오류 안내 6), 실패/오류/skip 0.
P0 모듈 URL 6개도 별도 빌드에서 통과했습니다. 중복된 URL 테스트를 별개 검증 항목처럼 합산하지 않습니다.
Firestore 테스트 10개는 실행하지 못했으며 PASS 수에 포함하지 않습니다.

## Firestore 규칙 테스트가 검사하는 시나리오

1. 코드 발급 원자성, 커플 목록 조회 차단.
2. 참여 시 profile과 couple의 동시 변경 강제.
3. 파트너 태그 읽기 허용, 개인 원본/프로필 읽기 거부.
4. 다른 커플의 읽기/쓰기 거부.
5. 파트너 태그 위조·공유 데이터에 URL 삽입·3인 참여 거부.
6. 비인증 접근 거부.
7. 해제 후 공유 데이터 읽기/쓰기 및 재활성화 거부.
8. 허용되지 않은 태그 및 비공개 제외 해시 삭제 거부.
9. 이벤트 작성자 위조·원본 URL 삽입·기존 이벤트 수정 거부.
10. 활성 커플에서 profile만 변경해 연결을 우회하는 행위 거부.

로컬 실행 명령: `cd firebase && npm ci && npm run verify:rules`. 자세한 절차는 `firebase/LOCAL-VERIFICATION.md` 참조.
차단 호스트: `storage.googleapis.com`.
공식 URL: `https://storage.googleapis.com/firebase-preview-drop/emulator/cloud-firestore-emulator-v1.19.8.jar`.
현재 환경 네트워크 상태를 재확인했으며 이 호스트는 허용 목록에 없습니다.
사용자는 현재 허용 도메인을 추가할 수 없다고 명시했습니다. 이 환경의 규칙 테스트는 BLOCKED로 유지하며 재다운로드·우회하지 않습니다. 공식 Emulator 사용 가능한 로컬 환경에서 별도로 실행합니다.

## 검증 전 배포 차단

`firebase/verification-status.json`은 BLOCKED입니다. Firebase predeploy hook이 PASS+현재 파일 해시 일치를 확인합니다.
배포 guard의 로컬 단위 테스트 3개가 통과했고, BLOCKED 검사 자체가 예상대로 exit 1로 종료했습니다.
이는 규칙의 실제 보안 검증 통과를 의미하지 않습니다. 실제 배포 명령은 실행하지 않았습니다.

## 완료 범위와 다음 검증

Firebase 설정 없이도 Fake 시연 APK는 실행 가능하도록 작성했고 Robolectric 시작 테스트가 통과했습니다.
후보는 기존 실제10개 URL이며 콘텐츠 태그는 사용자 수동 분류입니다. 자동 콘텐츠 분석/전체검색을 구현한 것처럼 표시하지 않습니다.
실제 Firebase 연결 후 Emulator 보안 테스트 → 두 폰 인증/연결/실시간 동기화/해제 → P0 재생 JSON 대조가 다음 단계입니다.
이 보고서의 '미검증'은 실패 또는 성공을 추정한 결과가 아닙니다.

## 기존 프로젝트 격리

FashionFit은 읽기와 기존 Gradle wrapper 사본 참조 외 수정하지 않았습니다.
기존 P0 app 모듈 소스도 변경하지 않았습니다. root settings에 mvp 및 probeOnly 선택만 추가했습니다.
P0 결과 JSON 필드/기록 기능은 그대로 유지했습니다.
