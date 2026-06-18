# Task-86: SocialNetworkRepositoryAdapter 비즈니스 정책 서비스 레이어 이동

## 배경

`SocialNetworkRepositoryAdapter`의 Cypher 쿼리 문자열과 어댑터 메서드 내부에
그래프 시각화 노출 정책과 프라이버시 정책 값이 하드코딩되어 있다.
정책 변경 시 어댑터(persistence 계층)를 직접 수정해야 하므로 계층 경계 위반이다.

---

## 현황 분석

### 문제 1 — 동적 엣지 한도 계산식 (`NETWORK_PRUNING_SUFFIX`)

```cypher
// SocialNetworkRepositoryAdapter.java L44
toInteger(5 + coalesce(item.friendship.intimacy, 0.0) * 10) AS dynamicLimit
```

"친밀도가 높을수록 시각화에서 더 많은 내부 엣지를 허용한다 (min=5, max=15)"는
그래프 노출 정책이다. 현재 이 공식이 Cypher 문자열에 매몰되어 있어
서비스 레이어에서 정책을 인지하거나 변경할 수 없다.

`SocialExpansionQueryService.calculateLimit()` / `calculateThreshold()` 가
유사한 친밀도 → 노출 범위 결정을 서비스 레이어에서 담당하는 것과 일관성이 없다.

---

### 문제 2 — 라벨 네트워크 circleSize 하드코딩

```java
// SocialNetworkRepositoryAdapter.java L179
.bind(DunbarCircle.DUNBAR.getLimitSize()).to("limitSize")
```

`getDefaultNetworkGraph`는 `DunbarCircle`을 파라미터로 받지만
`getLabelCustomNetwork`는 어댑터가 DUNBAR(150)를 직접 결정한다.
포트 시그니처가 비대칭이며, "라벨 네트워크는 항상 150명 상한"이라는
정책이 어댑터에 은닉되어 있다.

---

### 문제 3 — Stranger Quota (`GET_NETWORK_CONTACTS_OF_TWO_HOP`)

```cypher
// SocialNetworkRepositoryAdapter.java L126
ORDER BY tf.intimacy DESC
LIMIT 5
```

"2-hop 낯선 사람에게 공통 친구를 최대 5명까지만 노출한다"는 프라이버시 정책
(CLAUDE.md에 Stranger Quota로 명명)이 Cypher `LIMIT` 절에 하드코딩되어 있다.

---

## 변경 사항

### 1. `NETWORK_PRUNING_SUFFIX` 파라미터화

Cypher의 `5`와 `10`을 `$pruningMin`, `$pruningRange`로 교체한다.

```cypher
toInteger($pruningMin + coalesce(item.friendship.intimacy, 0.0) * $pruningRange) AS dynamicLimit
```

`SocialNetworkRepository` 포트의 두 네트워크 조회 메서드에
`int pruningMin`, `int pruningRange` 파라미터를 추가한다.

`SocialNetworkQueryService`에 상수와 계산 책임을 부여한다.

```java
private static final int PRUNING_EDGE_MIN   = 5;
private static final int PRUNING_EDGE_RANGE = 10;  // max = MIN + RANGE = 15
```

---

### 2. `getLabelCustomNetwork` 포트 시그니처 통일

포트 시그니처에 `DunbarCircle circleSize` 파라미터를 추가한다.

```java
// 변경 전
NetworkGraphResult getLabelCustomNetwork(Long userId, String labelId);

// 변경 후
NetworkGraphResult getLabelCustomNetwork(Long userId, String labelId, DunbarCircle circleSize);
```

어댑터에서 하드코딩을 제거하고 `$limitSize` 바인딩으로 교체한다.
서비스의 `getLabelNetwork`에서 기본값 `DunbarCircle.DUNBAR`를 명시적으로 전달한다.

---

### 3. Stranger Quota 파라미터화

Cypher의 `LIMIT 5`를 `LIMIT $strangerQuota`로 교체한다.

`SocialNetworkRepository` 포트의 `getNetworkContactsOfTwoHop`에
`int strangerQuota` 파라미터를 추가한다.

`SocialNetworkQueryService`에 상수를 정의한다.

```java
private static final int STRANGER_QUOTA = 5;
```

---

## 변경 파일 목록

| 파일 | 변경 내용 |
|------|-----------|
| `social/adapter/out/persistence/neo4j/SocialNetworkRepositoryAdapter.java` | Cypher 파라미터화 3건, 어댑터 메서드 시그니처 수정 |
| `social/application/port/out/SocialNetworkRepository.java` | 포트 시그니처 수정 (pruningMin/Range, circleSize, strangerQuota) |
| `social/application/service/SocialNetworkQueryService.java` | 상수 정의 및 레포지토리 호출 시 전달 |
| `social/adapter/in/web/SocialQueryController.java` | 영향 없음 (서비스 시그니처 변경 없음) |
| `social/adapter/in/web/SocialQueryControllerTest.java` | 서비스 목 호출 파라미터 업데이트 |

---

## 변경하지 않는 것

- 네트워크 쿼리 로직 자체 (Cypher 구조 변경 없음)
- API 엔드포인트 및 응답 형식
- `SocialExpansionRepositoryAdapter` (별도 어댑터, 범위 외)
- 실제 정책 값 (5, 10, 5) — 이동만 하고 값은 유지

---

## 브랜치

`ai/refactor-network-policy-to-service`
