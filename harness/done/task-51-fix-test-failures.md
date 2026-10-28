# Task-51: 누적 테스트 실패 수정

## 배경

task-46~50을 통해 프로덕션 코드가 다수 변경되었으나, 기존 테스트 코드가 갱신되지 않아
main 브랜치에서 23건의 테스트가 실패 중이다.

## 실패 목록 (23건)

| 테스트 클래스 | 실패 수 | 원인 |
|---|---|---|
| `FlagControllerTest` | 11 | `BaseControllerTest`에 `@MockitoBean FlagInvitationUseCase` 누락 (task-46) |
| `FlagCommentCommandServiceTest` | 4 | 서비스가 `FlagParticipantRepository` 의존 추가 + `existsById` → `findById` 변경됨 |
| `FlagCommentQueryServiceTest` | 3 | 서비스에 `validateAccess` 추가로 `FlagParticipantRepository` 스텁 필요 |
| `FlagQueryServiceTest` | 1 | `getFlagDetail`이 비참여자에게 `FlagAuthorizationException` throw로 변경 |
| `FlagDeletionEventListenerTest` | 1 | 리스너가 `FlagMemorialRepository` 대신 `FlagPreservationPolicy` 사용으로 변경 |
| `FriendRequestQueryServiceTest` | 2 | `FriendRequest` 생성자에서 `getId()` 호출 → mocked SocialUser에 stub 필요 |
| `TraceTest` | 1 | `TraceSelfVisitException` throw로 변경됐으나 테스트는 `IllegalArgumentException` 기대 |

## 수정 범위

### 1. `BaseControllerTest.java`
- `@MockitoBean FlagInvitationUseCase flagInvitationUseCase` 추가

### 2. `FlagCommentCommandServiceTest.java`
- `@Mock FlagParticipantRepository participantRepository` 추가
- `existsById` → `findById` 스텁 교체
- `createReply_Success`, `createReply_ToReply_ThrowsException`: `flagRepository.findById` + `participantRepository` 스텁 추가

### 3. `FlagCommentQueryServiceTest.java`
- `@Mock FlagParticipantRepository participantRepository` 추가
- 각 테스트에 `participantRepository.findAllParticipantIdsByFlagId` 스텁 추가
- `getCommentTree_PublicComments_VisibleToAll`: 뷰어를 VIEWER_ID로 변경 (validateAccess 통과)
- `getCommentTree_PrivateComment_FilteredForStranger`: 99L을 참여자로 추가하여 접근 허용 후 필터링 검증

### 4. `FlagQueryServiceTest.java`
- `getFlagDetail_ViewerHasNoRelation_ReturnsNullRole` → `FlagAuthorizationException` 기대로 변경

### 5. `FlagDeletionEventListenerTest.java`
- `@Mock FlagMemorialRepository` → `@Mock FlagPreservationPolicy` 교체
- `handleFlagDeletion_updatesParentPreservation`: `verify(flagPreservationPolicy).refresh(parentId)`로 변경

### 6. `FriendRequestQueryServiceTest.java`
- `FriendTestFactory.createRequest` 호출 전 `req.getId()`, `res.getId()` stub 추가

### 7. `TraceTest.java`
- `TraceSelfVisitException` import 추가
- `constructor_Fail_SelfVisit`: `IllegalArgumentException` → `TraceSelfVisitException` 변경

## 결과 기준

`./gradlew test` 전체 통과
