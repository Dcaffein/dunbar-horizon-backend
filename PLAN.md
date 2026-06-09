# PLAN: Task-59 — 유저 간 연결 중개인 탐색 API

## Branch
`ai/feat-connection-path-api` (from `main`)

## 목표
`GET /api/v1/networks/path?targetId={userId}` 엔드포인트 추가.
직접 연결 여부와 2-hop 공통 친구(중개인) 목록을 기하평균 점수 순으로 반환한다.

---

## 새 파일

### 1. `ConnectionPathResult.java`
```java
public record ConnectionPathResult(
    boolean direct,
    List<IntermediaryResult> intermediaries
) {
    public record IntermediaryResult(Long userId, String nickname, Double score) {}
}
```

### 2. `SocialConnectionPathRepository.java` (output port)
```java
public interface SocialConnectionPathRepository {
    List<ConnectionPathResult.IntermediaryResult> findIntermediaries(Long myId, Long targetId);
}
```

### 3. `SocialConnectionPathQueryUseCase.java` (input port)
```java
public interface SocialConnectionPathQueryUseCase {
    ConnectionPathResult getConnectionPath(Long myId, Long targetId);
}
```

### 4. `SocialConnectionPathQueryService.java`
`direct`는 `FriendshipRepository.existsById(compositeId)`로 확인 — 별도 경로 쿼리 없이 `Friendship.id` 인덱스 단순 조회.

```java
@Service
@RequiredArgsConstructor
@Neo4jTransactional(readOnly = true)
public class SocialConnectionPathQueryService implements SocialConnectionPathQueryUseCase {
    private final SocialConnectionPathRepository connectionPathRepository;
    private final FriendshipRepository friendshipRepository;

    @Override
    public ConnectionPathResult getConnectionPath(Long myId, Long targetId) {
        boolean direct = friendshipRepository.existsById(Friendship.generateCompositeId(myId, targetId));
        List<ConnectionPathResult.IntermediaryResult> intermediaries =
                connectionPathRepository.findIntermediaries(myId, targetId);
        return new ConnectionPathResult(direct, intermediaries);
    }
}
```

### 5. `SocialConnectionPathRepositoryAdapter.java`
`Neo4jClient` + raw Cypher 사용.
이유: 2-hop 경로에서 r2·r3·r4를 개별 명명해 `isRoutable`을 확인해야 하는데,
Cypher DSL의 `RelationshipChain.named()`는 chain 전체를 명명하므로 개별 관계 접근이 불가.
쿼리 내용이 동적 조건 없이 고정이라 raw string이 더 가독성 높다.

**2-hop 쿼리** (중개인 목록):
```cypher
MATCH (me:UserReference {id: $myId})-[:HAS_FRIENDSHIP]->(f1:Friendship)<-[r2:HAS_FRIENDSHIP]-(mid:UserReference)
      -[r3:HAS_FRIENDSHIP]->(f2:Friendship)<-[r4:HAS_FRIENDSHIP]-(target:UserReference {id: $targetId})
WHERE r2.isRoutable = true AND r4.isRoutable = true
WITH mid, sqrt(f1.intimacy * f2.intimacy) AS score
ORDER BY score DESC
RETURN mid.id AS userId, mid.nickname AS nickname, score
```

isRoutable 비대칭 적용:
- `me → f1` (나의 HAS_FRIENDSHIP): 무시 — 내 탐색이므로 내 설정은 관여하지 않음
- `r2` (mid의 me-mid Friendship 인식): 체크 — mid가 경유를 원하지 않으면 제외
- `r3` (mid의 mid-target Friendship 인식): 체크하지 않음
- `r4` (target의 mid-target Friendship 인식): 체크 — target의 노출 여부만 보호

---

## 수정 파일

### `SocialQueryController.java`
의존성 `SocialConnectionPathQueryUseCase` 추가, 엔드포인트 추가:
```java
@GetMapping("/path")
public ResponseEntity<ConnectionPathResult> getConnectionPath(
    @CurrentUserId Long currentUserId,
    @RequestParam Long targetId
) {
    return ResponseEntity.ok(connectionPathQueryUseCase.getConnectionPath(currentUserId, targetId));
}
```

---

## 결정 사항

- **1-hop isRoutable 체크 없음**: `direct` 플래그는 "우리가 이미 친구인가"를 나타내는 사실이므로 isRoutable과 무관.
- **3-hop 미포함**: task-59 문서 확정 사항.
- **`targetId = myId` 방어**: 서비스에서 `myId.equals(targetId)`이면 `new ConnectionPathResult(false, List.of())` 즉시 반환.
- **비활성 유저 자동 제외**: `UserReference` 레이블 MATCH — deactivate 시 레이블이 교체되므로 별도 필터 불필요.
