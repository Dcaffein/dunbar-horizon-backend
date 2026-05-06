# Task 22: 프로필 이미지 S3 업로드 연동

## Objective
현재 `PATCH /api/auth/users/me`는 클라이언트가 완성된 URL을 직접 전달하는 구조라 실제 파일 업로드 흐름이 없다.
사용자가 이미지를 업로드하면 서버가 S3에 저장하고 URL을 프로필에 반영해야 한다.

## Domain Change
[ ] 없음  [x] 있음

## Background
- buzz 도메인의 `ImageStoragePort`를 account에서 공유하지 않는다. 도메인별 포트·어댑터를 독립 구현한다.
- `MultipartFile`은 `adapter/in/web` 계층에서만 사용한다. UseCase 시그니처에 올라가지 않는다.
- 이미지 파일은 optional — 닉네임만 수정하는 경우도 정상 동작해야 한다.

## Decision
- `MultipartFile`을 UseCase까지 올리지 않으려면 컨트롤러와 서비스 사이에서 어떤 타입으로 변환할지 PLAN.md에서 제안한다.
- API Contract 변경: `application/json` → `multipart/form-data`. 기존 클라이언트 영향 범위 확인 필요.
