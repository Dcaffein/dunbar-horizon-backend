# PLAN: Task-51 누적 테스트 실패 수정 ✅ DONE

## 목표

main 브랜치에서 task-46~50 프로덕션 코드 변경으로 발생한 23건의 테스트 실패를 수정한다.
테스트 로직은 변경하지 않으며, 실제 서비스 동작에 맞게 테스트 스텁·어설션만 갱신한다.

---

## 수정 파일 목록

| 파일 | 변경 내용 |
|------|-----------|
| `support/BaseControllerTest.java` | `@MockitoBean FlagInvitationUseCase flagInvitationUseCase` 추가 |
| `flag/.../FlagCommentCommandServiceTest.java` | `@Mock FlagParticipantRepository` 추가; `existsById` → `findById` 스텁 교체; `createReply` 테스트에 `flagRepository` + `participantRepository` 스텁 추가 |
| `flag/.../FlagCommentQueryServiceTest.java` | `@Mock FlagParticipantRepository` 추가; 각 테스트에 `participantRepository` 스텁 추가; `getCommentTree_PublicComments` 뷰어 `99L` → `VIEWER_ID` |
| `flag/.../FlagQueryServiceTest.java` | `getFlagDetail_ViewerHasNoRelation`: `role = null` → `FlagAuthorizationException` throw 기대 |
| `flag/.../FlagDeletionEventListenerTest.java` | `@Mock FlagMemorialRepository` → `@Mock FlagPreservationPolicy`; verify 대상 변경 |
| `social/.../FriendRequestQueryServiceTest.java` | `FriendTestFactory.createRequest` 전 `req.getId()`, `res.getId()` stub 추가 |
| `trace/TraceTest.java` | `TraceSelfVisitException` import + `IllegalArgumentException` → `TraceSelfVisitException` |

---

## 브랜치

`ai/fix-test-failures` (from main)
