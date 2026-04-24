# PLAN: Social/Friend API 연동 완성 (task-01-SocialApi)

## [도메인 수정 승인 요청]

### 승인 요청 1: `DELETED` 상태 제거 + 물리 삭제 전환

**변경 배경:** `reject` 기능 제거, HIDDEN 요청은 시스템이 1개월 후 자동 삭제.
이에 따라 `DELETED` 상태가 존재할 이유가 없어지므로 도메인에서 제거하고 물리 삭제로 전환.

| 변경 대상 | 현재 | 변경 후 |
|-----------|------|---------|
| `FriendRequestStatus.DELETED` | soft-delete terminal 상태 | 제거 |
| `FriendRequest.cancel()` | status → DELETED | 메서드 제거 |
| `FriendRequest.complete()` | status → DELETED | 메서드 제거 |
| `FriendRequestRepository` | — | `deleteById(String id)` 추가 |
| `FriendRequesterActionService.cancelRequest()` | status 변경 후 save | 물리 삭제 호출 |
| `FriendRequestReceiverActionService.acceptRequest()` | complete() 후 save | establish 후 물리 삭제 |

ACCEPTED, HIDDEN, PENDING 상태 및 기존 hide/undoHide/accept 전이는 그대로 유지.

---

### 승인 요청 2: `DeletableFriendship` 제거 + 어댑터 버그 수정

**현재 버그:**
`FriendshipRepositoryAdapter.delete()` (line 40)에서
`friendshipNeo4jRepository.delete(friendship)` 호출 시 `DeletableFriendship` 객체를
그대로 전달하고 있음. `FriendshipNeo4jRepository`는 `Neo4jRepository<Friendship, String>`을
상속하므로 `delete(Friendship)` 타입을 기대 → **타입 불일치 버그**.

**DeletableFriendship 필요성 검토:**
- 의도: `checkDeletable(myId)` 호출 후에만 삭제 가능하도록 타입으로 강제
- 그러나 `findById(userId, friendId)`는 `generateCompositeId(userId, friendId)`로 ID를
  계산하므로, 존재하면 이미 두 유저 중 하나가 호출자임이 보장 → `checkDeletable()` 권한 검증이 **중복**
- Adapter에서는 `friendship.getEntity()`로 내부 `Friendship`을 꺼내야 하므로 오히려 혼란

**결론: 제거 권장**

| 변경 대상 | 현재 | 변경 후 |
|-----------|------|---------|
| `FriendshipRepository.delete()` | `delete(DeletableFriendship)` | `delete(String friendshipId)` |
| `FriendshipRepositoryAdapter.delete()` | 타입 불일치 버그 | `deleteById(friendshipId)` 호출 |
| `Friendship.checkDeletable()` | DeletableFriendship 반환 | 메서드 제거 |
| `FriendshipCommandService.brokeUpWith()` | checkDeletable() 경유 | ID 직접 전달 |
| `DeletableFriendship.java` | Wrapper 클래스 | 파일 제거 |
| `global/common/Deletable.java` | 추상 기반 클래스 | 다른 곳 미사용 시 제거 |

---

## 목표

`social/friend` 도메인에 구현되어 있으나 Controller까지 연결되지 않은 기능을 연동하고,
자연스럽게 있어야 하는 CRUD를 추가한다.

---

## 작업 계획 (7 Items + 1 Bugfix)

### Item 0: DeletableFriendship + checkDeletable() 제거 + 어댑터 버그 수정 [도메인 수정]

변경 파일:
- `domain/friend/Friendship.java` — `checkDeletable()` 메서드 제거
- `domain/friend/DeletableFriendship.java` — 파일 제거
- `domain/friend/repository/FriendshipRepository.java` — `delete(DeletableFriendship)` → `delete(String friendshipId)`
- `adapter/out/FriendshipRepositoryAdapter.java` — `friendshipNeo4jRepository.deleteById(friendshipId)` 호출
- `application/service/FriendshipCommandService.java` — `checkDeletable()` 제거, `delete(friendshipId)` 직접 호출
- `global/common/Deletable.java` — 다른 도메인에서 미사용 확인 후 제거

---

### Item 1: `getTwoHopSuggestionsByOneHop()` Controller 연결

**추가 endpoint:** `GET /api/v1/networks/suggestions/pivot?pivotId={id}`

변경 파일:
- `adapter/in/web/SocialQueryController.java` — endpoint 추가

---

### Item 2: HIDDEN 친구 요청 목록 조회

**추가 endpoint:** `GET /api/v1/friend-requests/hidden`

변경 파일:
- `application/port/in/FriendRequestQueryUseCase.java` — `getHiddenRequests(userId)` 추가
- `application/service/FriendRequestQueryService.java` — 구현 추가
- `adapter/in/web/FriendRequestController.java` — endpoint 추가

---

### Item 3: 보낸 친구 요청 목록 조회

**추가 endpoint:** `GET /api/v1/friend-requests/sent`

변경 파일:
- `domain/friend/repository/FriendRequestRepository.java` — `findAllByRequester_IdAndStatus()` 추가
- `adapter/out/FriendRequestRepositoryAdapter.java` — 구현 추가
- `application/port/in/FriendRequestQueryUseCase.java` — `getSentRequests(userId)` 추가
- `application/service/FriendRequestQueryService.java` — 구현 추가
- `adapter/in/web/FriendRequestController.java` — endpoint 추가

---

### Item 4: 특정 라벨 상세 조회

**추가 endpoint:** `GET /api/v1/labels/{labelId}`

변경 파일:
- `application/port/in/LabelQueryUseCase.java` — `getLabelById(ownerId, labelId)` 추가
- `application/service/LabelService.java` — 구현 추가
- `adapter/in/web/LabelController.java` — endpoint 추가

---

### Item 5: 특정 라벨 멤버 목록 조회

**추가 endpoint:** `GET /api/v1/labels/{labelId}/members`

반환: 멤버 프로필 (userId, nickname, profileImageUrl) — `FriendProfileInfo` 재사용

변경 파일:
- `application/port/in/LabelQueryUseCase.java` — `getLabelMembers(ownerId, labelId)` 추가
- `application/service/LabelService.java` — 구현 추가
- `adapter/in/web/LabelController.java` — endpoint 추가

---

### Item 6: `DELETED` 상태 제거 및 물리 삭제 리팩토링 [도메인 수정]

변경 파일:
- `domain/friend/FriendRequestStatus.java` — `DELETED` 상태 제거, `cancel()` / `complete()` default 구현 제거
- `domain/friend/FriendRequest.java` — `cancel()`, `complete()` 메서드 제거
- `domain/friend/repository/FriendRequestRepository.java` — `deleteById(String id)` 추가
- `adapter/out/FriendRequestRepositoryAdapter.java` — 물리 삭제 구현
- `application/service/FriendRequesterActionService.java` — `deleteById()` 호출
- `application/service/FriendRequestReceiverActionService.java` — `deleteById()` 호출

---

### Item 7: HIDDEN 요청 1개월 후 자동 삭제 스케줄러

**기준:** `FriendRequest.createdAt` 기준 1개월 경과 + status = HIDDEN

변경 파일:
- `domain/friend/repository/FriendRequestRepository.java` — `deleteOldHiddenRequests(LocalDateTime threshold)` 추가
- `adapter/out/FriendRequestRepositoryAdapter.java` — Cypher 쿼리 구현
- `adapter/in/scheduler/FriendRequestCleanupScheduler.java` — 신규. 매일 새벽 4시 실행

---

## 예상 사이드 이펙트

- Item 0: `Deletable.java` 제거 전 다른 도메인 사용 여부 확인 필요.
- Item 6 (물리 삭제 전환): 기존에 DELETED 상태로 Neo4j에 저장된 레코드가 잔존할 수 있으나 신규 흐름에는 영향 없음.
- 나머지 항목은 신규 경로 추가로 기존 기능에 영향 없음.

---

## 브랜치

`ai/feat-social-api-wiring`
