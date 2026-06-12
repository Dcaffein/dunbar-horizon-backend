# Task 65 — currentSkeletonIds 보안 검증 및 외부인 엣지 탐색 제한

## Objective

`/mutual/one-hop`, `/mutual/two-hop` 엔드포인트의 skeleton 계산 방식을 클라이언트 전달 방식(`currentSkeletonIds`)으로 전환하되, 두 가지 문제를 함께 해결한다.

1. **보안**: 클라이언트가 전달한 `currentSkeletonIds`가 실제 나의 친구인지 서버에서 검증하여, 임의 ID로 타인의 소셜 그래프를 탐색하는 공격을 차단한다.
2. **프라이버시 강화**: skeleton 노드들은 circleSize 바깥의 외부인이므로, 해당 노드에서 뻗어나가는 엣지 수를 기존 5~30(일반 네트워크)보다 엄격한 5~10으로 제한한다.

## 배경 및 이유

### 현재 구조의 문제

현재 `buildCurrentSkeleton`은 `labelId` + `circleSize`를 기반으로 서버에서 skeleton을 재계산한다. 이는 화면에 동적으로 추가된 노드(드래그앤드롭으로 circleSize 바깥에서 추가된 친구)를 반영하지 못한다. 클라이언트가 현재 화면의 실제 skeleton을 직접 전달해야 정확한 엣지 조회가 가능하다.

### 보안 취약점

`currentSkeletonIds`를 검증 없이 사용하면, 공격자가 임의의 ID를 전달해 `target`과의 엣지 응답 여부로 "target이 user X와 친구인가?"를 열거할 수 있다. `isRoutable=true`인 관계는 모두 노출된다.

**방어**: 서버에서 `currentSkeletonIds ∩ 내 실제 친구 ID 목록`으로 교집합을 취해 검증된 ID만 쿼리에 사용한다.

### dynamicLimit 엄격화

circleSize 바깥 외부인 노드는 친밀도가 낮을 가능성이 높고, 프라이버시 보호 측면에서도 노출 엣지 수를 제한하는 것이 적절하다.

- 기존 일반 네트워크: `5 + intimacy * 25` → 5~30
- 외부인 skeleton 노드: `5 + intimacy * 5` → 5~10

## 변경 대상 (탐색 전 예비 목록)

| 파일 | 예상 변경 |
|------|---------|
| `social/adapter/in/web/SocialQueryController.java` | `labelId`, `circleSize` 파라미터 → `currentSkeletonIds: List<Long>` 로 교체 |
| `social/application/port/in/SocialNetworkQueryUseCase.java` | `getNewNodeEdges`, `getNetworkContactsOfTwoHop` 시그니처 변경 |
| `social/application/service/SocialNetworkQueryService.java` | skeleton 검증 로직 추가 (friendshipRepository로 내 친구 ID 조회 후 교집합) |
| `social/application/port/out/SocialNetworkRepository.java` | 포트 시그니처 변경 |
| `social/adapter/out/persistence/neo4j/SocialNetworkRepositoryAdapter.java` | `buildCurrentSkeleton` 제거, 검증된 ID 목록을 IN 절로 직접 사용. dynamicLimit 공식 5~10 적용 |

## Out of Scope

- 기존 `buildDynamicPruningNetwork` (일반 네트워크 조회) 변경 없음
- `circleSize`, `labelId` 파라미터 제거에 따른 다른 엔드포인트 영향 없음

## Edge Cases

- `currentSkeletonIds`가 비어있거나, 교집합 결과가 빈 리스트이면 빈 배열 반환
- `currentSkeletonIds`의 모든 ID가 내 친구가 아닌 경우도 빈 배열 반환 (예외 없이 처리)
