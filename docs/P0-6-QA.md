# CoupleReels P0-6 검증 결과 — 2026-11-14 KST

## 기준과 범위

기준: P0-5 `70129d1a9ca174ba882a7be66461dc8b00672f79`.
작업 브랜치: `feature/p0-6-validation-20261114`.
원격 기준 SHA를 재확인했습니다. 기본/P0-4/P0-5 브랜치를 변경·병합하거나 강제 푸시하지 않습니다.
P0-5의 크림·핑크 대신 이번 요청의 **버건디×골드**를 적용했습니다.
현재 APK는 Firebase 설정이 없는 **같은 기기 내부 로컬 체험**입니다.
운영 Firebase 배포, 운영 데이터 삭제, 운영 권한 변경은 수행하지 않았습니다.

## 실제 화면 7장

Android 10(API 29) AOSP x86_64, 320×640 실행 캡처입니다. Galaxy 실기기 캡처가 아닙니다.
별도 목업·이미지 합성·영상 썸네일 조작·계정 데이터 주입 없이, 설치 APK에서 테스트가 가입/선택/저장/연결을 조작했습니다.
서로 다른 QA 계정은 UI로 생성했고 기존 사용자 데이터는 초기화하지 않았습니다.

| 전달 이미지 | 실행 화면 |
|---|---|
| 00_Onboarding_Taste_Step1.png | 관심사 온보딩 |
| 01_Discover_A1_Reels.png | 상대방이 보낸 최대 5개 후보 결과; 영상 재생 성공 표시는 없음 |
| 02_My_Taste.png | 본인 취향·선물 선호·공유 진입 |
| 03_Saved.png | 실제로 저장한 테스트 링크·관심/삭제 제어 |
| 04_Us_Unlinked.png | 미연결 상태·초대 코드·개인 기능 계속 |
| 05_Onboarding_Taste_Step2.png | 스타일·선물 취향 |
| 06_Reel_Share_BottomSheet.png | 실제 ACTION_SEND 수신 후 URL·태그·저장/취소 |

캡처 방식은 기기 compositor의 UiAutomation.takeScreenshot입니다. 공개 URL의 이미지나 콘텐츠를 임의로 만들지 않았습니다.
화면의 데이터는 로컬 QA 입력이며 실제 Instagram 영상 내용 검증을 뜻하지 않습니다.

버건디 배경 `#2C0D1B`, 카드 `#511C32`, 골드 `#E6C788`, 아이보리 글자 `#FFF4E2`를 사용했습니다.
공유 시트의 검은 상태 표시줄 아이콘 문제를 실제 화면에서 발견해 밝은 아이콘으로 수정했습니다.
임베드의 흰 WebView가 부모 영역 밖을 가리는 문제는 View/Compose bounds clipping과 래퍼 배경으로 보완했습니다.
긴 내용은 스크롤로 접근합니다. 작은 화면에서 줄바꿈은 허용하며 하단 탭은 유지합니다.
기본 글꼴·320×640 범위에서 조작/스크롤을 검사했습니다. 확대 글꼴, 실제 IME, Galaxy 크기/세로 스와이프는 사용자 확인이 남아 있습니다.
발견의 작은 안내 카드는 내부 스크롤이 필요할 수 있으며 이 화면을 실제 영상 전체화면 재생으로 소개하지 않습니다.

## 검사 결과

| 검사 | 결과와 근거 |
|---|---|
| P0-1 URL 회귀 | PASS 6, FAIL 0, SKIP 0 |
| MVP JVM 회귀 | PASS 41, FAIL 0, SKIP 0: 기존 36 + 개인정보 5 |
| Firebase 배포 guard 단위 검사 | PASS 3, FAIL 0, SKIP 0 |
| 로컬 전체 사용자 시나리오 | PASS 1: 최종 APK의 온보딩→공유→연결→추천→보관→해제 |
| 기존 UI 6개 목적의 네이티브 검사 | PASS 6: 기동 1, 공유/연결 2, 온보딩/취소/재생성 2, 기록 내보내기 1 |
| 원래 Robolectric 전체 실행 방식 | BLOCKED: Maven 대용량 JAR 리디렉션 대상 gcp.mirrors.maven.org의 proxy 403 |
| Firestore 규칙 | BLOCKED 13: 공식 JAR storage.googleapis.com 다운로드가 proxy 403 |
| 빌드/테스트 코드 컴파일/서명 | PASS |
| P0-1 Lint | 오류 0, 경고 5 |
| MVP Lint | 오류 0, 경고 13 |
| 배포 guard | 예상대로 BLOCKED/exit 1. 미검증 새 규칙을 PASS로 편집하지 않음 |
| Android 15 AOSP | 환경 문제로 기기 러너 시작 실패. NetworkStack/SystemUI/전화 등 시스템 ANR 관찰 |
| Galaxy S25+ / 실제 두 폰 | BLOCKED: 연결된 실기기·운영 설정 없음 |

최종 Android 10 통합 실행은 `OK (7 tests)`이며 원문은 전달 폴더 `native-final/device-test.log`입니다.
JVM 검사 실행 합계는 50회지만 P0-1 URL 6개가 MVP에도 포함되어 고유 검사는 44개입니다.
Robolectric을 통과했다고 주장하지 않습니다. 네이티브 대체 검사는 실제 Android 10에서 같은 목적을 확인한 별도 실행입니다.
Firestore 테스트는 원본이 실제 11개였으며 10개 고정 검증 래퍼 오류를 고쳤습니다. 개인정보 검사 2개를 추가해 13개를 요구합니다.
경고는 오류로 숨기지 않았으며 기존 P0-1 모듈의 코드는 변경하지 않았습니다.

## 핵심 기능 판정

| 기능 | 상태 | 실제 검증 범위 |
|---|---|---|
| 취향 온보딩 | PASS | UI 2단계, 선택 전 저장 비활성, 개인 저장, 4탭 전환 |
| Android Instagram 링크 공유 수신 | PASS | text/plain 및 image/jpeg의 Reel URL, 정규화, 로그인 전 유지, 중복, 취소, 회전/재생성. 실제 Instagram 앱 Sharesheet는 별도 확인 |
| 취향 태그·보관함 | PASS | 로컬 개인 저장, 관심/중복 상태와 계정 분리 회귀. 운영 저장은 미검증 |
| 커플 연결/해제 | PASS | 같은 기기의 두 QA 계정, 코드 복사/입력, 해제 후 피드 차단. 실제 두 폰 서버 연결은 BLOCKED |
| 상대 추천 피드 | PASS | 작성자 기기의 계산 결과만 전달. 최대 5개, 원본 제외, 원래 태그·해시 미전달. 유사 영상 정확도·전체 Instagram 수집 기능은 아님 |
| 실제 릴스 재생 | BLOCKED | 공식 임베드 요청 후 링크만 보임. 움직이는 영상/소리 미확인. host의 embed.js 요청은 proxy 403; 실제 폰 네트워크 경로는 별도 검사 필요 |
| 선물/기념일 취향 태그 | PASS | 선택·저장과 태그 기반 후보 순위 계산 범위 |
| 기념일 날짜/알림·선물 상품 추천 | FAIL — 미구현 | 일정 모델·알림·상품 DB/추천 서비스가 없음. 기존 태그를 해당 기능 완성으로 표현하지 않음 |
| Firestore 실제 권한 차단 | BLOCKED | 코드·JVM 불변식은 보완했지만 공식 규칙 실행과 운영 적용은 미완료 |

## 실패를 분리한 과정

1. P0-5 기준 검사를 먼저 재실행하고 원문을 보관했습니다.
2. Android 15 소프트웨어 기기에서 시스템 시작 지연/ANR과 러너 종료가 발생했습니다. KVM이 없고 충분한 메모리에서도 시스템 앱이 실패했습니다.
3. 지원 범위 안의 Android 10에서 네이티브 Compose 검사로 전환했습니다.
4. 첫 캡처는 SystemUI ANR에 가려 정상 화면으로 채택하지 않았습니다. 시스템 대화상자에서 실제 Wait를 선택한 뒤 다시 캡처했습니다. 앱 ANR을 자동으로 무시하지 않습니다.
5. 공유 시트의 “목걸이”와 뒤 화면의 같은 문구 때문에 테스트 선택자가 중복됐습니다. 공유 태그/4탭에 고유 선택자를 사용했습니다. 앱 기능 실패와 구분했습니다.
6. 사용자 단계 완료 후 ActivityScenario 정리 오류를 진단했습니다. 공유가 Activity Intent의 action/type을 바꾸면 AndroidX가 초기 Intent와 달라진 lifecycle을 무시합니다. 검사의 모든 assertion 후 초기 Intent 메타데이터만 복원하고, 재생성 검사 직전에도 추적 메타데이터를 정리했습니다. 실제 데이터나 assertion을 지우지 않았습니다.
7. 실제 화면에서 발견한 시트 상태 표시줄·WebView 영역 문제를 수정하고 재빌드/검사를 반복했습니다.
8. 최종 단위/네이티브 결과는 이전 실패 횟수를 합산해 부풀리지 않고 마지막 그룹 실행 기준으로 기록합니다. 실패 원문도 전달 묶음에 보존합니다.

## 개인정보와 운영 적용

[개인정보 계약](P0-6-PRIVACY.md)을 따릅니다. 기존 태그/이벤트는 본인만 읽고 새 쓰기를 막습니다.
새 추천 문서는 URL·갱신 시각만 갖고 결과는 최대 5개입니다. 입력 태그·해시·점수·이유를 보내지 않습니다.
전체 풀의 9개를 내보내 빠진 원본을 특정하는 위험도 줄였습니다. 추천 결과로 취향을 추측할 가능성은 남습니다.
운영에 새 규칙을 적용하지 않았으므로 구버전 서버의 기존 데이터가 이미 차단됐다고 주장하지 않습니다.
기존 데이터 삭제 없이 이전하고 실제 두 계정 권한 검사·운영 적용 검토·별도 배포 승인이 필요합니다.

## 재현 및 남은 외부 조치

Linux/Cloud, 완전한 JDK 17+, SDK 35/Build Tools 35, Node 22 및 npm 의존성을 준비합니다.
현재 Cloud: `source /workspace/tooling/activate.sh`.

```sh
# 정상 네트워크에서 원래 전체 단위/빌드 검사
bash qa/run-p06-regression.sh
# 네트워크 차단 환경에서 명시적 JVM 부분집합 + Lint + APK
PURE_JVM=1 GRADLE_BIN=/workspace/tooling/gradle-8.11.1/bin/gradle bash qa/run-p06-regression.sh
# 전용 로컬 기기에서 네이티브 7개 검사와 실제 캡처
bash qa/run-p06-device.sh SERIAL OUTPUT
# 공식 다운로드 허용 후 규칙 13개 (운영 배포 아님)
cd firebase
npm run verify:rules
```

Firebase CLI 캐시는 FIREBASE_EMULATORS_PATH, 설정은 XDG_CONFIG_HOME을 쓰기 가능한 경로로 지정해야 하는 환경일 수 있습니다.
기기 스크립트는 am instrument의 shell exit 0만 믿지 않고 OK (7 tests), Process crashed/FAILURES 여부를 확인합니다.
데이터 삭제·앱 제거·외부 앱 메시지 전송을 하지 않습니다. 내보내기 검사는 Chooser를 취소하도록 가로채 실제 전송하지 않습니다.

환경 초안에 기존 설정을 보존해 gcp.mirrors.maven.org, storage.googleapis.com, www.instagram.com, instagram.com을 추가했습니다.
현재 실행 정책에는 아직 적용되지 않았습니다. 설정 검토·저장 후 새 연결에서 다시 검사해야 합니다.
Instagram 미디어 CDN은 실제 리디렉션을 확인해 필요한 목적지만 추가해야 하며 광범위한 우회 허용을 하지 않습니다.

발표 순서는 [P0-6-DEMO.md](P0-6-DEMO.md)를 사용합니다. 운영 동기화·실제 영상·기념일/상품 추천 미완료를 명확히 구분합니다.

## 최종 APK 식별

- 패키지: `com.couplereels.mvp`, versionCode `6`, versionName `0.6-private-projections`
- APK SHA-256: `9ebc724301e7fa2b5fed4acad7248646e8ca1cb957279a1c3d577553b1cf63ed`
- 설치된 base.apk의 SHA-256도 동일합니다. P0-5와 같은 Android debug 인증서입니다.
- 실제 캡처 7장은 최종 APK 실행 원본이며, 재생 진단 8번째 이미지는 별도 보관합니다.
- 320dp에서 긴 제목은 줄바꿈되며 하단 버튼은 스크롤해야 합니다. 큰 글꼴·키보드·Galaxy 기기 배치는 별도 확인이 필요합니다.
