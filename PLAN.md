# PLAN: Social 네트워크 그래프 조회 Redis 캐시 도입 (task-23)

## 작업 목표
`getDefaultIntimacyNetwork`, `getLabelCustomNetwork`에 Look-Aside Redis 캐시를 도입하여 Neo4j 부하 방어 및 응답 속도 개선.

---

## [도메인 수정 승인 요청]
- `social/domain/label/event/LabelMemberChangedEvent.java` 신규 생성 (라벨 멤버 변동 이벤트)
- `LabelService`에 `ApplicationEventPublisher` 주입 및 이벤트 발행 로직 추가

---

## 현황 분석

### 이벤트 현황
| 이벤트 | 발행 위치 | 상태 |
|---|---|---|
| `FriendRequestAcceptedEvent` | `FriendRequestReceiverActionService` | 이미 발행 중 |
| `FriendShipDeletedEvent` | `FriendshipCommandService` | 이미 발행 중 |
| 라벨 멤버 변동 이벤트 | — | **없음 → 신규 생성 필요** |

### API URL 불일치 문제
현재 라벨 네트워크 API는 `{labelName}`을 경로 변수로 사용한다.
```
GET /api/v1/networks/labels/{labelName}
```
반면 task-23 결정사항의 캐시 키는 `labelId` 기반이다. `LabelRepository`에 이름으로 ID를 조회하는 메서드가 없고, 추가하더라도 캐시 조회마다 Neo4j 룩업이 발생한다.

**결정**: API 경로를 `{labelId}`로 변경한다.
- 다른 라벨 관련 API(`/labels/{labelId}/members` 등)와 일관성이 생김
- 라벨 네트워크 Neo4j 쿼리도 `labelId`로 필터링하도록 변경
- 프론트엔드는 라벨 목록 조회(`GET /api/v1/labels`) 응답의 labelId를 사용

### UseCase 인터페이스 타입 불일치
현재 `SocialNetworkQueryUseCase.getFriendsNetwork(Long userId, int limitSize)`는 `int`를 받는다. 캐시 키에 `DunbarCircle` enum이 필요하므로 인터페이스 시그니처를 변경한다.

---

## 변경 파일 목록

### 신규 생성

| 파일 | 역할 |
|---|---|
| `global/config/RedisConfig.java` | RedisTemplate Bean, Jackson JSON 직렬화 설정 |
| `social/domain/label/event/LabelMemberChangedEvent.java` | 라벨 멤버 변동/삭제 도메인 이벤트 `record(Long ownerId, String labelId)` |
| `social/application/port/out/SocialNetworkCacheRepository.java` | 캐시 조회·저장·삭제 Output Port |
| `social/adapter/out/cache/SocialNetworkCacheAdapter.java` | Redis 구현체, 패턴 기반 키 삭제 포함 |
| `social/adapter/in/event/SocialNetworkCacheEvictListener.java` | `@TransactionalEventListener(AFTER_COMMIT)` 무효화 리스너 |

### 수정

| 파일 | 변경 내용 |
|---|---|
| `build.gradle` | `spring-boot-starter-data-redis` 의존성 추가 |
| `application.yml` | Redis 연결 설정 추가 (`localhost:6379` 기본값) |
| `SocialNetworkQueryUseCase.java` | `getFriendsNetwork` 시그니처: `int limitSize` → `DunbarCircle circleSize` |
| `SocialNetworkQueryService.java` | Look-Aside 패턴 적용, `getLabelNetwork` 파라미터 `labelName` → `labelId` |
| `SocialNetworkRepository.java` (port) | `getLabelCustomNetwork` 파라미터 `labelName` → `labelId` |
| `SocialNetworkNeo4jRepositoryAdapter.java` | `getLabelCustomNetwork` Cypher 필터를 `name` → `id` 기반으로 변경 |
| `SocialQueryController.java` | `getFriendsNetwork`: `circleSize` 직접 전달 / 라벨 경로 `{labelName}` → `{labelId}` |
| `LabelService.java` | `addMemberToLabel`, `removeMemberFromLabel`, `replaceLabelMembers`, `deleteLabel`에 `LabelMemberChangedEvent` 발행 추가 |

---

## 구현 방향

### 캐시 키
```
dunbar:network:default:{userId}:{circleSize}   ex) dunbar:network:default:42:DUNBAR
dunbar:network:label:{userId}:{labelId}        ex) dunbar:network:label:42:abc-123
```

### SocialNetworkCacheRepository Port
```java
Optional<List<NetworkFriendEdgeResult>> getDefaultNetwork(Long userId, DunbarCircle circleSize);
void putDefaultNetwork(Long userId, DunbarCircle circleSize, List<NetworkFriendEdgeResult> result);
void evictDefaultNetwork(Long userId);         // dunbar:network:default:{userId}:* 전체 삭제

Optional<List<NetworkFriendEdgeResult>> getLabelNetwork(Long userId, String labelId);
void putLabelNetwork(Long userId, String labelId, List<NetworkFriendEdgeResult> result);
void evictLabelNetwork(Long userId, String labelId);    // 단일 키 삭제
void evictAllLabelNetworks(Long userId);                // dunbar:network:label:{userId}:* 전체 삭제
```

### Look-Aside 패턴 (SocialNetworkQueryService)
```
캐시 조회 → hit: 즉시 반환 / miss: Neo4j 조회 → 캐시 저장 → 반환
```

### 무효화 리스너 (AFTER_COMMIT)
| 이벤트 | 처리 |
|---|---|
| `FriendRequestAcceptedEvent` | 양측 `evictDefaultNetwork` |
| `FriendShipDeletedEvent` | 양측 `evictDefaultNetwork` + `evictAllLabelNetworks` |
| `LabelMemberChangedEvent` | `evictLabelNetwork(ownerId, labelId)` |

### 패턴 삭제 구현
`RedisTemplate.keys(pattern)` 대신 `scan` 커서 방식으로 구현하여 운영 환경 블로킹 방지.

---

## 예상 사이드 이펙트
- 라벨 네트워크 API URL 변경(`{labelName}` → `{labelId}`) — 프론트엔드 수정 필요
- `SocialNetworkQueryUseCase` 인터페이스 변경 — 구현체(`SocialNetworkQueryService`) 외 참조처 없으므로 영향 없음

---

## 테스트 전략
기본 프로토콜을 따르되, Redis 의존 통합 테스트는 Testcontainers `redis` 컨테이너를 사용.
