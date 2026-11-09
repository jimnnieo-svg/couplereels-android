# CoupleReels P0-2/3 사용·개발 안내

## 현재 구현

Firebase Authentication 이메일/비밀번호 가입·로그인·로그아웃, 사용자 프로필, 32자리 128-bit 커플 연결 코드,
두 명 제한 연결, 연결 해제, 태그/공유 이벤트 저장, 파트너 최신 취향 snapshot listener,
후보 10개 추천·원본 제외·중복 제거·저장·관심 표시를 구현했습니다.
실제 Firebase 프로젝트 연결/실기기 실행은 아직 검증하지 않았습니다.

`CoupleRepository`를 `FakeRepository`와 `FirebaseRepository`가 구현합니다.
Fake는 로컬 JSON과 세션 UID를 SharedPreferences에 저장합니다. JVM 테스트에서 두 Repository가 동일 Backend를 공유하여
두 계정 이벤트 전달을 검증하지만 다른 폰 간 동기화의 증거는 아닙니다.
Fake 비밀번호는 테스트 전용 SHA-256 해시로 저장하며 운영 인증에 사용하면 안 됩니다.
Firebase 모드에서 비밀번호는 앱 코드가 저장하지 않고 Firebase Auth SDK가 인증 세션을 유지합니다.
Firestore는 memory cache로 설정하여 파트너 데이터를 앱 종료 후 디스크 캐시에 남기지 않습니다.
서버 권한 오류/연결 해제/로그아웃 때 화면의 파트너 데이터를 비웁니다.

## Firebase 연결에 필요한 사용자 작업

1. 무료 Spark 범위의 별도 CoupleReels Firebase 프로젝트를 선택/생성합니다. 유료 API/Functions는 사용하지 않습니다.
2. Android 앱을 **com.couplereels.mvp**로 등록합니다. P0-1 Probe의 패키지 **com.couplereels.app**과 다릅니다.
3. 해당 앱의 `google-services.json`을 `mvp/google-services.json`에 배치합니다. 서비스 계정 키는 필요 없습니다.
4. Authentication → Sign-in method에서 Email/Password를 활성화합니다.
5. Firestore 보안 규칙은 로컬 공식 Emulator 검증이 끝날 때까지 배포하지 않습니다. 공개 test mode 규칙을 사용하지 마세요.
6. `./gradlew :mvp:assembleDebug`로 다시 빌드합니다. 이 버전은 설정 포함 시 Firebase 모드로 시작합니다.
7. 폰 2대에 같은 프로젝트 설정을 포함한 APK를 설치합니다.

**현재 규칙 검증 상태는 BLOCKED이며 운영 배포는 금지합니다.**
공식 Emulator를 사용할 수 있는 로컬 PC에서 `firebase/LOCAL-VERIFICATION.md` 절차로 검증합니다.
현재 환경에서 도메인 제한을 우회하거나 다운로드를 재시도하지 않습니다.

```sh
cd firebase
npm ci
npm run verify:rules
npm run check:deployment
```

첫 번째 검증 명령은 공식 Emulator에서 demo-couplereels 테스트만 실행하며 실제 프로젝트에 배포하지 않습니다.
두 번째 검사는 PASS 결과와 현재 규칙·테스트·의존성·설정 파일의 SHA-256 일치를 요구합니다.
Firebase predeploy hook에도 이 검사를 연결했습니다. 이는 실수 방지 장치이며 클라우드 IAM 권한 통제를 대체하지 않습니다.
테스트 통과 자체가 운영 배포 승인은 아닙니다. 현재 작업에서는 배포를 수행하지 않습니다.

## Firestore 데이터 모델

- `users/{uid}`: uid, email, name, coupleId. 본인만 읽기/쓰기.
- `users/{uid}/saved/{sha256Url}`: url, tags, liked, privateSource. 본인만 읽기/쓰기. 원본 URL은 여기만 저장.
- `couples/{32hexCode}`: members(1~2개 uid), active.
- `couples/{code}/tastes/{uid}`: tags, excludedIds(URL SHA-256), updatedAt(server timestamp). 연결된 두 사용자만 읽기, 본인만 쓰기.
- `couples/{code}/events/{autoId}`: authorId, tags, updatedAt. 이벤트 이력에는 원본 URL을 넣지 않음. 수정·삭제 불허.

코드 발급/참여는 couple 및 내 profile을 한 트랜잭션에서 변경해야 규칙이 허용합니다.
대기 코드 GET은 코드를 가진 미연결 인증 사용자에게만 허용합니다. 커플 목록 조회는 불허합니다.
코드는 고엔트로피 초대 토큰이며 소유한 사람이 참여할 수 있으므로 상대방에게만 직접 전달하세요.
명시적 연결 해제로 코드가 폐기됩니다. 자동 만료는 이번 버전에 없습니다.
연결 해제는 couple.active=false를 변경하며 재활성화할 수 없습니다.
상대방 프로필은 변경하지 않고 실시간 couple 상태에서 연결 종료를 반영합니다.
이전 연결은 기록으로 남지만 tastes/events 읽기는 즉시 불허합니다. 새 연결은 새 코드로 만듭니다.
개인 저장 데이터는 연결 해제 후에도 유지됩니다.

## 추천과 후보 태그

기존 16개 후보 PoC 소스를 현재 작업공간에서 찾지 못해 가중 Jaccard 태그 유사도를 새로 구현했습니다.
silver/necklace=1.4, gift=1.2, anniversary=1.3, 나머지=1.0.
교집합 가중치 합 / 합집합 가중치 합으로 계산하고 동점은 정규화 URL 순으로 정렬합니다.
입력은 HTTPS 개별 Reel URL만 허용하고 tracking query를 제거하여 중복을 합칩니다.
상위 최대10개만 반환합니다. 비공개 원본의 SHA-256과 일치하는 후보는 제외합니다.
해시는 URL의 직접 노출을 막기 위한 것이며, 알려진 후보 목록을 대입하면 일치 여부를 추론할 수 있습니다.

기존10개 Reel URL은 유지했지만 영상 내용 태그를 자동 생성/임의 지정하지 않았습니다.
처음에는 모든 후보가 미분류이고 동점입니다. '취향/공유'에서 선택한 태그로 피드의 각 후보를 **직접 수동 분류**해야
실제 태그 기반 순위 차이가 생깁니다. 후보 분류 메타데이터는 로그인 계정별 이 기기에 저장합니다.
두 폰에서 같은 랭킹을 보려면 동일하게 분류해야 합니다. 공용 검증 태그 카탈로그는 후속 작업입니다.
Instagram 전체 자동 검색/스크래핑/AI 영상 분석 기능은 없습니다.

## Fake 모드 시연 (폰 1대)

1. a@example.test / 테스트 비밀번호로 A 가입 → 이름 저장 → 연결 코드 발급 → 코드 복사.
2. 마이페이지 로그아웃 → b@example.test로 B 가입 → A의 코드로 연결.
3. A로 다시 로그인 → 취향/공유에서 silver, necklace를 고르고 URL1을 비공개로 저장.
4. B로 로그인 → 상대방 태그 수신 확인. URL1은 피드에서 제외되는지 확인.
5. B가 취향/공유 화면에서 silver를 선택한 뒤 피드 URL2에 '선택 태그로 이 후보 수동 분류'.
6. date를 선택하고 URL3을 수동 분류.
7. A가 최신 취향을 silver → date로 변경. B 피드가 URL2 → URL3 순으로 바뀌는지 확인.
8. B가 저장/관심/삭제 동작 확인. 앱 종료·재실행 후 B 로그인 상태와 저장 항목 확인.
9. 연결 해제 후 양쪽 피드에서 파트너 데이터가 사라지고 코드 재사용이 실패하는지 확인.

## Firebase 모드 폰 2대 시연

설정 포함 APK 설치 → A/B 각각 가입 → A 코드 발급 → B 연결 → A 태그/URL 저장 →
B 피드의 실시간 태그 갱신 확인 → B 후보 수동 분류 → A 취향 변경 후 B 순위 변화 →
저장/관심 → 앱 재시작 로그인 확인 → A 연결 해제 후 B 데이터 비움 확인.
다른 커플 C/D로 A/B 문서의 읽기/쓰기 실패도 별도 테스트합니다.
실제 Reel 재생 테스트는 P0-1 화면에서 별도 JSON으로 기록합니다.

## P0-1 보존

`app` 소스 6개는 원본 ZIP과 바이트 비교해 동일함을 확인했습니다.
원본 APK·ZIP·QA.md·README.md는 `/workspace/output`에서 덮어쓰지 않습니다.

```sh
./gradlew -PprobeOnly=true :app:testDebugUnitTest :app:assembleDebug
```

위 옵션은 mvp 모듈을 설정에서 제외하여 Firebase 플러그인 없이 Probe만 빌드합니다.
MVP의 'P0-1 독립 재생 검증 열기' 및 피드의 '앱 안에서 재생'은 원래 Probe Activity를 실행합니다.
Probe JSON 필드/저장 방식은 변경하지 않았습니다.

## 알려진 제한

실기기 WebView 재생/메모리/화면 이동, 실제 Firebase 인증과 두 폰 동기화, 실제 서버 규칙 적용은 미검증입니다.
비공개 제외 해시는 커플별 최대100개이며 해당 연결 동안 제거할 수 없습니다.
서버 응답이 25초를 넘으면 UI에서 시간 초과를 표시합니다. Firebase 작업 자체는 완료될 수 있으므로 상태 확인 후 재시도합니다.
회원가입 중 Auth만 성공하고 프로필 저장이 실패했다면 로그인 후 마이페이지의 이름 저장으로 프로필을 복구할 수 있습니다.
운영용 계정 삭제/비밀번호 재설정/자동 초대 만료/앱 체크 및 악용 방지 운영 기능은 이번 범위에 포함하지 않았습니다.
