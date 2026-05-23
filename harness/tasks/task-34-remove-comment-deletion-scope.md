# Task-34: CommentDeletionScope 제거

## 배경

`CommentDeletionScope`는 댓글 삭제 시 대댓글 포함 여부(`includeReplies`)를 전달하기 위해 도입된 래퍼 클래스다.
현재 로직은 다음과 같다.

- 루트 댓글 삭제 → `issueDeletionScope()`가 `includeReplies = true` 반환
- 대댓글 삭제 → `issueDeletionScope()`가 `includeReplies = false` 반환

그러나 Repository 구현체(`FlagCommentRepositoryAdapter`)에서 두 경우 모두 결국 같은 JPQL 쿼리를 통해 처리할 수 있다.

```sql
DELETE FROM FlagComment c WHERE c.id = :id OR c.parentId = :id
```

- 루트 댓글이면 → `c.id = :id` + `c.parentId = :id`(대댓글들) 모두 삭제
- 대댓글이면 → `c.id = :id`만 매칭 (parentId 일치하는 자식 없음)

`parentId`에 인덱스가 걸려있다면 성능 문제 없고, 댓글 삭제는 저빈도 작업이다.
따라서 `includeReplies` 분기 자체가 불필요하며 `CommentDeletionScope` 클래스를 제거한다.

## 목표

- `CommentDeletionScope` 클래스 삭제
- `FlagCommentRepository` 포트 인터페이스 단순화: `delete(CommentDeletionScope)` → `deleteWithReplies(Long commentId)`
- `FlagCommentRepositoryAdapter` 에서 분기 로직 제거, 단일 경로로 통일
- `FlagComment`에서 `issueDeletionScope()` 제거, `validateDeletionAuthority()`는 유지
- `FlagCommentCommandService` 직접 `validateDeletionAuthority` 호출 후 `deleteWithReplies` 호출

## 변경 파일

| 파일 | 변경 내용 |
|------|----------|
| `flag/domain/comment/CommentDeletionScope.java` | **삭제** |
| `flag/domain/comment/FlagComment.java` | `issueDeletionScope()` 메서드 제거 |
| `flag/domain/comment/repository/FlagCommentRepository.java` | `delete(CommentDeletionScope)` → `deleteWithReplies(Long commentId)` |
| `flag/adapter/out/persistence/FlagCommentRepositoryAdapter.java` | 분기 제거, `deleteTargetAndReplies(commentId)` 단일 호출 |
| `flag/application/service/comment/FlagCommentCommandService.java` | `issueDeletionScope` 제거, `validateDeletionAuthority` + `deleteWithReplies` 직접 호출 |

## 체크리스트

- [ ] `CommentDeletionScope.java` 삭제
- [ ] `FlagComment.java` — `issueDeletionScope()` 제거
- [ ] `FlagCommentRepository.java` — 포트 메서드명 변경
- [ ] `FlagCommentRepositoryAdapter.java` — `deleteWithReplies` 구현, 분기 제거
- [ ] `FlagCommentCommandService.java` — 서비스 로직 단순화
- [ ] 테스트 코드 업데이트 (FlagCommentCommandServiceTest 등)
- [ ] 빌드 확인

## 브랜치

`ai/refactor-remove-comment-deletion-scope`
