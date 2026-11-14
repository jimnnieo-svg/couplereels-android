# P0-6 발표용 체험 모드 — 2026-11-19

## 1분 시연

1. CoupleReels 실행 → **로그인 없이 체험하기**.
2. 관심사에서 **향수** 또는 **여행** 선택 → 다음 → 스타일 선택 → **취향 저장하고 시작하기**.
3. 발견에서 **내 취향 추천**, **데모 콘텐츠**, **내 취향과 맞는 이유**를 보여준다. 위로 넘겨 다른 예시를 확인한다.
4. Instagram에서 공개 릴스의 공유 메뉴 → CoupleReels. 또는 취향 탭에 실제 개별 릴스 URL을 입력한다.
5. 태그 선택 → **내 취향에 반영하기** → 보관함에 저장된 실제 URL을 확인한다.
6. 발견으로 돌아가 추가된 취향을 반영한 순서를 확인한다. 앱을 닫고 다시 열어 취향과 보관함 유지도 보여준다.

이미 체험 중이면 바로 발견 화면이 열린다. 첫 화면을 다시 보여주려면 **우리 → 체험 설정 → 체험 나가기**.
이 동작은 취향과 보관함을 삭제하지 않는다. 기존 체험에서 다시 들어오면 온보딩을 반복하지 않으므로
**취향 → 취향 수정**으로 선택 과정을 보여줄 수 있다.

## 사실대로 설명할 범위

- 실제 동작: 로그인 없는 진입, 로컬 저장/복원, 취향 선택, 4탭, 공유 인텐트 수신, URL 검증·정규화,
  중복 공유 태그 병합, 보관함, 선택 태그에 따른 가중 유사도 계산과 예시 카드 정렬.
- 데모 콘텐츠: 자체 작성한 제목·설명·태그 카드. 실제 영상, 실제 Instagram 검색 결과, 실제 상대 계정이 아니다.
  가짜 영상 썸네일이나 재생 성공 상태를 표시하지 않는다.
- 사용자 저장 릴스: 실제로 입력·공유한 URL. 공식 임베드 열기, Instagram에서 열기, 링크 복사 제공.
  영상 재생은 공개 범위·로그인·네트워크·Instagram 정책에 영향을 받는다. 페이지 로드와 영상 재생 성공을 구분한다.
- 계정 서비스: Firebase 클라이언트 설정이 없는 APK는 로그인/계정 생성 제출을 비활성화하고 짧게 안내한다.
  회원가입 화면 자체는 정상 이동한다. 가짜 계정을 만들지 않는다.
- 실제 두 폰 연결·서버 동기화는 이번 체험 모드 범위 밖이다. 기존 Firebase 구현과 P0-1 재생 검증은 보존했다.

## 저장과 데이터 보존

`DemoStore.NAME = couplereels-guest-p06-v1`의 앱 전용 SharedPreferences에 체험 활성 여부와 JSON snapshot을 저장한다.
snapshot은 취향, 실제 저장 링크, 관심 여부, 온보딩 완료 여부를 가진다. 이메일·비밀번호·인증 UID는 만들지 않는다.
저장 실패 시 화면에 성공 상태를 내보내지 않는다. 체험 모드에서는 FirebaseRepository를 만들지 않는다.

기존 `couplereels-demo-v1` 저장소와 Firebase 계정 데이터는 이 저장소로 복사하거나 합치지 않는다.
기존 계정 데이터를 삭제하지 않는다. 기존 백업·기기 전송 제외 설정도 유지한다.

## 개발 도구

일반 화면의 개발 진단 버튼은 제거했다. Debug 빌드에서 상단 CoupleReels 브랜드를 길게 누르면
P0-1 독립 재생 검증 화면을 열 수 있다. 일반 사용 흐름에 진단 문구를 노출하지 않는다.

## 아이콘 이력 — 변경 없음

- 최초 추가: 2026-11-10 18:22:10 KST.
- 커밋: `33c78c25adf44fd016f4b8b8b777243b99aaff65`.
- 제목: `feat(mvp): receive Instagram Reel shares and save private tastes before pairing`.
- 파일: `mvp/src/main/res/drawable/ic_couplereels.xml`.
- Manifest: `android:icon="@drawable/ic_couplereels"`.
- 분류: Android 기본 런처 아이콘이 아닌, 두 개의 링과 하트 경로를 직접 정의한 벡터 drawable.
  Git 기록으로 파일 추가 이력은 확인되지만 사용자의 디자인 승인 여부까지 증명할 수는 없다.
- P0-6에서 아이콘 파일과 아이콘 연결은 변경하지 않았다.

## 빌드

JDK 17, Android SDK 35, Build Tools 35.0.0, 저장소 Gradle Wrapper 8.11.1.
Windows에서는 영문 경로의 별도 작업 폴더를 사용한다.

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:ANDROID_HOME='C:\Users\USER\AppData\Local\Android\Sdk'
& "$env:JAVA_HOME\bin\java.exe" -classpath gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain --no-daemon --max-workers=2 :mvp:assembleDebug :app:testDebugUnitTest :mvp:testDebugUnitTest :mvp:lintDebug
```

패키지는 `com.couplereels.mvp`, versionCode는 6, versionName은 `0.6-demo`다.
기존 앱과 서명이 같을 때만 `adb install -r`로 데이터 보존 업데이트한다. 서명 충돌을 앱 삭제로 해결하지 않는다.
APK 파일명은 `CoupleReels-P0-6-Demo-debug.apk`. P0-5 APK는 보존한다.
