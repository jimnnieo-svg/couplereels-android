# 공식 Firestore Emulator를 사용할 수 있는 로컬 환경의 검증

현재 클라우드의 규칙 테스트 상태: **BLOCKED**.
원인: storage.googleapis.com 네트워크 정책 차단. 사용자가 도메인 추가 불가를 확인함.
현재 클라우드에서 다운로드 재시도·우회·실제 Firebase 배포를 수행하지 않습니다.

## 준비

- Node.js 22 LTS, npm, JDK 21 (`java -version` 확인).
- 저장소의 최신 소스를 로컬에 복사.
- 공식 Firebase Emulator가 이미 설치되어 있거나 공식 다운로드가 허용되는 로컬 네트워크.
- localhost:8085 포트가 비어 있어야 함.
- Firebase 계정, google-services.json, 서비스 계정, 유료 프로젝트는 테스트에 필요 없음.

## 실행 (프로젝트 루트에서)

```sh
cd firebase
npm ci
npm run test:gate
npm run verify:rules
npm run check:deployment
```

`verify:rules`는 다음 공식 CLI 명령을 실행합니다.

```sh
npx firebase emulators:exec --only firestore --project demo-couplereels "node --test --test-reporter=tap rules.test.cjs"
```

연결은 127.0.0.1:8085만 사용하고 실제 프로젝트 데이터에 접근하지 않습니다.
10개 규칙 시나리오를 모두 실행합니다. 다른 커플 접근/비공개 원본/이벤트 위조/연결 해제 등을 검사합니다.
`verification-status.json`은 실행 전 RUNNING, 종료 실패 시 FAIL, 명령 성공 및 소스 불변 시 PASS가 됩니다.
검증 시점의 rules/test/package-lock/firebase.json SHA-256도 기록합니다.
`npm run check:deployment`는 BLOCKED/FAIL/RUNNING/보고서 누락/소스 변경 시 exit 1입니다.
현재 저장된 BLOCKED 보고서를 사람이 PASS로 편집하지 마세요.

## 완료 판정

- 공식 Emulator 규칙 테스트 10개가 실패·skip 없이 모두 통과해야 함.
- 검증된 최신 verification-status.json과 터미널 테스트 결과를 함께 보관.
- Android의 다른 커플 접근·두 폰 동기화·연결 해제도 별도 실제 프로젝트/기기에서 확인.
- 테스트 통과는 배포 승인이 아닙니다. 이번 작업에서는 운영 배포를 수행하지 않습니다.
- predeploy 검사 hook은 실수 방지 장치입니다. 설정을 우회할 권한이 있는 사람을 기술적으로 막는 IAM 경계는 아닙니다.
