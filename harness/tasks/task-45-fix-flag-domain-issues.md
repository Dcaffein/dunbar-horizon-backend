# task-45: Flag 도메인 취약점 수정

## 배경
Flag 도메인에서 접근 권한 검증 누락, 입력 검증 누락, bare orElseThrow 문제가 발견됨.

## 목표
- `Flag` 도메인에 접근/댓글 작성 권한 메서드 추가 (서비스 → 도메인 위임)
- 플래그 상세 조회 및 댓글 조회·작성 시 참여자 권한 검증 적용
- 컨트롤러 입력 검증(`@Valid`) 추가
- `deleteComment`의 bare `orElseThrow()` → `FlagNotFoundException`으로 수정

## 브랜치
`ai/fix-flag-domain-issues`

## 결과
- `Flag.validateAccess`, `Flag.validateCommentCreation` 도메인 메서드 추가
- `FlagQueryService`, `FlagCommentQueryService`, `FlagCommentCommandService` 권한 검증 적용
- `FlagCommentController` 3개 엔드포인트에 `@Valid` 추가
