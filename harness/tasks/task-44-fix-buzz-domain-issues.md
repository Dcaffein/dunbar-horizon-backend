# Task-44: Buzz 도메인 버그 수정 및 도메인 규칙 강화

## 배경

Buzz 도메인 코드 리뷰에서 발견된 버그 4건과 설계 개선 1건.

1. **isPublic 댓글 미구현** — `BuzzComment`에 `isPublic` 필드가 있으나 조회 시 필터링 없이 모든 접근자에게 노출됨
2. **자기 자신에게 댓글 알림** — Buzz 작성자가 자신의 Buzz에 댓글을 달면 본인에게 알림 발송
3. **S3 고아 파일** — `upload()` 이후 도메인 검증 실패 시 이미 업로드된 파일을 회수할 수 없음
4. **`validateCommentDeletion` 만료 체크 누락** — `createComment`, `updateComment`와 달리 만료 여부를 확인하지 않음
5. **Application layer 권한 체크** — `deleteBuzz`, `getBuzzDetail`의 권한 검증 로직이 서비스 계층에 위치; 도메인 규칙이므로 `Buzz` 엔티티로 이동

## 목표

- `isPublic=false` 댓글은 Buzz 작성자와 댓글 작성자만 볼 수 있도록 구현
- 도메인 검증 통과 후에만 S3 업로드 실행
- 만료·권한 검증을 도메인에 일관되게 집중

## 브랜치

`ai/fix-buzz-domain-issues`

## 결과

**상태:** 완료 (main 머지)

| 커밋 | 내용 |
|------|------|
| `9ab20a2` | isPublic 댓글 필터링, S3 업로드 순서 변경, 도메인 권한 검증 이동, 자기 알림 제거 |

**변경 내역:**
- `Buzz.java` — `getVisibleComments`, `validateAccess`, `validateDeletion`, `validateCommentCreation`, `validateCommentUpdate` 추가, `validateCommentDeletion`에 만료 체크 추가
- `BuzzDetailResult.java` — `getComments()` → `getVisibleComments(currentUserId)`
- `BuzzService.java` — upload 이전 사전 검증, 인라인 권한 체크 → 도메인 위임
- `BuzzNotificationDispatcher.java` — `creatorId == commenterId` 시 알림 스킵
