# P0-3 실기기 시연 준비

## 공통

최신 com.couplereels.mvp APK는 P0-2를 업데이트 설치할 수 있습니다(동일 debug 서명).
com.couplereels.app P0 Probe와도 함께 설치할 수 있습니다. Android8(API26) 이상 필요.
실제 Firebase 로그인에는 mvp/google-services.json을 포함한 재빌드가 필요합니다.
서비스 계정 키와 개인 서명 키는 앱/업로드에 포함하지 않습니다.

## Fake 시연: 폰1대에서 두 테스트 계정

1. A 가입→연결코드발급→코드복사→마이페이지 로그아웃.
2. B 가입→코드연결. 실제 폰 간 전송으로 보고하지 않습니다.
3. A 로그인→silver 태그와 URL1 공유. 비공개 체크 시 URL1은 B 피드에서 제외됩니다.
4. B 로그인→최신 상대방 태그와 추천 개수 확인. 후보는 우선 미분류입니다.
5. B가 silver를 선택하고 후보 URL2를 수동분류. date를 선택하고 후보 URL3를 수동분류.
6. A 취향 silver→date 변경. B의 추천 1위가 바뀌는지 확인.
7. B 저장·관심→저장 반복 후 관심 유지 확인→앱내 재생 진입.
8. 연결해제 후 피드 비움, 앱재시작 후 로그인/저장 복원 확인.

## 폰2대 Firebase 시연: 현재 BLOCKED인 조건

Firebase 클라이언트 설정과 규칙의 로컬 공식 Emulator 검증이 먼저 필요합니다.
규칙 검증 전 운영 환경 배포를 수행하지 않습니다. firebase/LOCAL-VERIFICATION.md 참고.
검증/설정 후 A/B 각각 별도 폰 로그인→A코드발급→B연결→A공유→B피드 자동갱신 순으로 확인합니다.
다른 커플 C/D의 문서 접근 거부도 실제 서버에서 별도 확인합니다.

## P0-1 재생 결과 기록

MVP 'P0-1 독립 재생 검증 열기' 또는 피드의 '앱 안에서 재생'을 누릅니다.
일반 웹/공식 임베드 각각 실제10개 URL을 직접 재생한 후 결과를 기록합니다.
페이지 로드, Compose/Robolectric 테스트 통과는 영상 재생 성공이 아닙니다.
'결과 기록'으로 PASS/FAIL/BLOCKED/미확인 중 실제 관찰값을 선택하고 '기록 내보내기'로 JSON을 전달합니다.
JSON에는 device/android/webview/url/mode/result/note/events 등이 유지됩니다.
비밀번호·인증코드 등 비밀정보를 메모에 입력하지 마세요.
