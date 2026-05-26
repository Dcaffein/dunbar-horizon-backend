# Task-40: FlagComment createReply 불필요한 Lock 제거

## 배경

`createReply`에서 depth 제한 검증(`parentId != null`)을 위해 `findByIdForUpdate`로 부모 comment에 락을 걸고 있다. 그러나 `parentId`는 생성 이후 변경되지 않는 불변값이라 동시 요청 간 race condition이 없다. 락이 보호하는 상태가 존재하지 않는다.

또한 `createRootComment`, `createReply` 모두 `@Transactional`이 누락되어 있다.

## 목표

- `findByIdForUpdate` → `findById`로 교체
- `createRootComment`, `createReply`에 `@Transactional` 추가

## 브랜치

`ai/fix-flag-comment-lock`

## 결과

**상태:** 완료 (main 머지)

| 커밋 | 내용 |
|------|------|
| `9103b30` | `findByIdForUpdate` → `findById`, `@Transactional` 추가, 예외 타입 수정 |

**변경 내역:**
- `FlagCommentCommandService.createReply` — `findByIdForUpdate` → `findById`, `@Transactional` 추가
- `FlagCommentCommandService.createRootComment` — `@Transactional` 추가
- `createReply` 예외 타입 `FlagNotFoundException` → `FlagCommentNotFoundException`으로 수정
- `FlagCommentCommandServiceTest` — `createReply` 관련 테스트 3개 stub 및 예외 타입 동기화
