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
