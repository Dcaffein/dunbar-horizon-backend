# Task-119: 관계망 조회의 boundary 포함 판정을 elementId 맵 조회로 전환

## Background

`SocialNetworkRepositoryAdapter`의 `NETWORK_PRUNING_SUFFIX`는 기본 네트워크(`GET_DEFAULT_NETWORK_GRAPH`)와 라벨 네트워크(`GET_LABEL_CUSTOM_NETWORK`)가 공유한다. 내부 연결을 고를 때 `targetMember IN boundary`로 상대가 boundary에 속하는지 판정한다.

boundary는 노드 151개짜리 리스트라 판정마다 선형 비교가 일어난다. 판정에 들어오는 행은 대부분 탈락하므로, 거의 모든 행이 리스트 끝까지 비교하고 나서야 탈락한다.

PROFILE의 DB Hits는 저장소 접근만 센다. 리스트 비교는 저장소를 거치지 않으므로 이 비용이 DB Hits에 잡히지 않는다. 그래서 단순화한 쿼리로 네 가지 표현을 PROFILE과 시간 양쪽으로 비교했다.

| 표현 | DB Hits | 클라이언트 p50 | 같은 라운드에서 list와의 차이 |
|------|--------:|------------:|------------------------|
| `t IN boundary` | 280,274 | 65.24ms | 기준 |
| `map[toString(t.id)]` | 326,042 | 64.77ms | −0.33ms, 56.3% 라운드에서 빠름 |
| `map[elementId(t)]` | 280,737 | 61.04ms | −4.01ms, 87.8% 라운드에서 빠름 |
| `apoc.coll.containsSorted(ids, t.id)` | 371,537 | 294.17ms | +228.90ms |

측정 조건은 대상 유저 1명(id=3, 친구 190명), 페이지 캐시 상주, 동시성 1, 1,000라운드 교차 실행이다. 네 변형의 실행 계획은 membership Filter 한 줄을 빼고 연산자 단위로 같았다. DB Hits로는 elementId 맵이 0.17% 손해였지만 시간으로는 약 6% 이득이었다.

## Objective

boundary 포함 판정을 elementId를 키로 하는 맵 조회로 바꿔, 판정마다 일어나는 선형 비교를 없앤다. 기본 네트워크와 라벨 네트워크의 반환 결과는 바뀌지 않는다.

## Decision

### 키는 `elementId(node)`다

`toString(node.id)`는 행마다 프로퍼티 읽기가 붙어 list와 구별되지 않았다. `apoc.coll.containsSorted`는 비교 횟수는 가장 적지만 4.5배 느렸다. 함수 호출마다 인자 리스트를 변환하는 비용으로 추정한다.

### elementId의 안정성 제약에 걸리지 않게 쓴다

elementId는 트랜잭션 사이에서 같은 값이 보장되지 않는다. 맵을 만드는 것과 조회하는 것이 같은 쿼리 안에서 일어나므로 이 제약에 해당하지 않는다. 맵이나 elementId 값을 쿼리 밖으로 내보내거나 캐시하지 않는다.

### 적용 범위는 `NETWORK_PRUNING_SUFFIX` 한 곳이다

두 네트워크 쿼리가 함께 바뀐다.

## Out of Scope

- 관심도 전달용 `interestMap`(`toString(id)` 키 맵)의 정리. 별건으로 다룬다.
- `$baseNetworkFriendIds` 파라미터 리스트로 판정하는 엣지 조회 쿼리(`GET_TWO_HOP_CONTACT_EDGES_FOR_TARGET`, `GET_DIRECT_FRIEND_EDGES_FOR_TARGET`)
- 데이터 모델 변경

## Acceptance Criteria

- `NETWORK_PRUNING_SUFFIX`에 `IN boundary` 판정이 남지 않는다.
- `SocialNetworkRepositoryAdapterTest`가 전부 통과한다. 기본 네트워크와 라벨 네트워크의 반환 결과가 변경 전과 같다는 것은 이 테스트로 확인한다.

성능은 Background의 측정으로 판단이 끝났으므로 다시 재지 않는다.
