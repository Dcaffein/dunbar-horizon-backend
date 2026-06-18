# PLAN — Task-86: SocialNetworkRepositoryAdapter 비즈니스 정책 서비스 레이어 이동

## 작업 목표

`SocialNetworkRepositoryAdapter`의 Cypher 문자열 및 어댑터 내부에 하드코딩된
그래프 노출 정책·프라이버시 정책 값 3건을 서비스 레이어로 이동한다.

---

## 현황 분석

### 문제 1 — `NETWORK_PRUNING_SUFFIX` L44
```cypher
toInteger(5 + coalesce(item.friendship.intimacy, 0.0) * 10) AS dynamicLimit
```
- min=5, range=10 (max=15)이 Cypher에 매몰
- "친밀도가 높을수록 내부 엣지를 더 많이 허용"이라는 그래프 노출 정책인데 서비스가 인지 불가
- `SocialExpansionQueryService.calculateLimit()` 이 동일 성격의 결정을 서비스에서 담당하는 것과 일관성 없음

### 문제 2 — `getLabelCustomNetwork` L179
```java
.bind(DunbarCircle.DUNBAR.getLimitSize()).to("limitSize")
```
- "라벨 네트워크는 항상 150명 상한" 정책을 어댑터가 직접 결정
- `getDefaultNetworkGraph`는 `DunbarCircle`을 파라미터로 받아 포트 시그니처 비대칭
- 컨트롤러 주석("최대 150명(Dunbar's Ceiling)까지 단일 뷰로 제공")이 확인하듯 정책값 자체는 DUNBAR 유지, **위치**만 서비스로 이동

### 문제 3 — `GET_NETWORK_CONTACTS_OF_TWO_HOP` L126
```cypher
LIMIT 5
```
- Stranger Quota(CLAUDE.md에 명명된 프라이버시 정책)가 Cypher `LIMIT` 절에 매몰

---

## 변경 파일 목록

| 파일 | 변경 내용 |
|------|-----------|
| `social/application/port/out/SocialNetworkRepository.java` | 세 메서드 시그니처에 정책 파라미터 추가 |
| `social/application/service/SocialNetworkQueryService.java` | 정책 상수 3개 정의, 레포지토리 호출 시 전달 |
| `social/adapter/out/persistence/neo4j/SocialNetworkRepositoryAdapter.java` | Cypher 파라미터화 3건, 메서드 시그니처 수정 |
| `social/adapter/in/web/SocialQueryControllerTest.java` | `getLabelNetwork` 성공 케이스 1건 추가 |

---

## 구현 방향

### `SocialNetworkRepository` 포트 시그니처 변경

```java
// 변경 전
NetworkGraphResult getDefaultNetworkGraph(Long userId, DunbarCircle circleSize);
NetworkGraphResult getLabelCustomNetwork(Long userId, String labelId);
List<NetworkOneHopsByTwoHopResult> getNetworkContactsOfTwoHop(Long userId, Long targetId, List<Long> skeletonIds);

// 변경 후
NetworkGraphResult getDefaultNetworkGraph(Long userId, DunbarCircle circleSize, int pruningMin, int pruningRange);
NetworkGraphResult getLabelCustomNetwork(Long userId, String labelId, DunbarCircle circleSize, int pruningMin, int pruningRange);
List<NetworkOneHopsByTwoHopResult> getNetworkContactsOfTwoHop(Long userId, Long targetId, List<Long> skeletonIds, int strangerQuota);
```

### `SocialNetworkQueryService` 상수 및 호출 변경

```java
private static final int PRUNING_EDGE_MIN   = 5;
private static final int PRUNING_EDGE_RANGE = 10;  // max = 5 + 10 = 15
private static final int STRANGER_QUOTA     = 5;
```

- `getFriendsNetwork` → `getDefaultNetworkGraph(..., PRUNING_EDGE_MIN, PRUNING_EDGE_RANGE)`
- `getLabelNetwork` → `getLabelCustomNetwork(..., DunbarCircle.DUNBAR, PRUNING_EDGE_MIN, PRUNING_EDGE_RANGE)`
- `getNetworkContactsOfTwoHop` → `getNetworkContactsOfTwoHop(..., STRANGER_QUOTA)`

### `SocialNetworkRepositoryAdapter` Cypher 수정

```cypher
-- 문제 1
toInteger($pruningMin + coalesce(item.friendship.intimacy, 0.0) * $pruningRange) AS dynamicLimit

-- 문제 3
LIMIT $strangerQuota
```

- 문제 2: `getLabelCustomNetwork`에서 `.bind(DunbarCircle.DUNBAR.getLimitSize())` 제거,
  파라미터로 수신한 `circleSize.getLimitSize()`를 바인딩

### `SocialNetworkQueryUseCase`(port/in)과 `SocialQueryController`는 변경하지 않는다.

정책 파라미터는 서비스 내부에서 결정하며 컨트롤러까지 노출할 이유가 없다.

---

## 예상 사이드 이펙트

- `SocialNetworkRepository` 포트 시그니처 변경 → 구현체가 어댑터 1개뿐이므로 컴파일 에러 범위 한정
- `@Cacheable` 키·동작 변경 없음 (캐시 파라미터는 `userId`, `circleSize`, `labelId`로 유지)

---

## 테스트 전략

`SocialQueryControllerTest`에 `getLabelNetwork` 성공 케이스 1건 추가.
나머지는 기존 통합 테스트가 정책 상수 이동을 커버한다.
