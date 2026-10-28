# Task: Social 네트워크 그래프 조회 Redis 캐시 도입

## Background
* DunbarHorizon의 소셜 네트워크 뷰는 인간의 인지적 한계(던바의 수) 철학을 반영하여, 친밀도를 기준으로 노드 바운더리를 잡고 엣지 수를 동적으로 제한(Dynamic Pruning)하는 무거운 Neo4j 연산을 수행함.
* 사용자의 조회가 발생할 때마다 실시간으로 연산하면 DB 서버의 CPU 및 I/O에 치명적인 부하가 발생함.
* 일반적인 그룹 필터링과 달리, 라벨 커스텀 네트워크는 그룹 내의 끈끈한 결속력을 보여주는 용도이므로 인원수별 슬라이더를 적용하지 않음. 단, 프론트엔드 렌더링 한계를 방어하기 위해 아무리 큰 라벨이라도 최대 150명(Dunbar's Ceiling)까지만 연산하여 제공함.

## Objective
* 가장 무거운 쿼리인 getDefaultIntimacyNetwork와 getLabelCustomNetwork의 연산 비용을 방어하고 API 응답 속도를 극적으로 개선하기 위해 Look-Aside 패턴의 Redis 캐시를 도입함.
* 친밀도/관심도의 점진적 변화는 TTL에 맡겨 지연 동기화하고, 토폴로지 변경(친구 추가/삭제)은 이벤트 주도 방식으로 즉시 무효화하여 성능과 정합성의 최적 균형을 맞춤.

## Domain Change
[ ] 없음  [x] 있음 (캐시 무효화를 트리거하기 위해, 친구 맺기/끊기 및 라벨 멤버 변동 시 Spring ApplicationEvent 발행 로직 추가 필요)

## Decision
* 캐시 데이터 구조: List<NetworkFriendEdgeResult> 객체를 순수 JSON String으로 직렬화하여 저장하며, 모든 캐시의 TTL은 10분(600초)으로 고정.
* Default Network 정책: 서버 사이드 슬라이싱 없이 4개 계층(5, 15, 50, 150) 각각 독립 캐싱.
    * Key: dunbar:network:default:{userId}:{circleSize}
* Label Network 정책: 150명 천장 규칙을 적용해 라벨당 단일 뷰만 전체 캐싱.
    * Key: dunbar:network:label:{userId}:{labelId}
* 무효화(Eviction) 정책: DB 트랜잭션이 완전히 커밋된 이후(@TransactionalEventListener(phase = AFTER_COMMIT))에 동작해야 함.

    | 시나리오 | Default Network | Label Network |
    |---|---|---|
    | 친구 추가 | 양측 유저 `default:*` 삭제 | 없음 (새 친구는 아직 어느 라벨에도 속하지 않음) |
    | 친구 삭제 | 양측 유저 `default:*` 삭제 | 양측 유저 `label:*` 삭제 |
    | 라벨 멤버 추가 | 없음 | 해당 `label:{labelId}` 삭제 |
    | 라벨 멤버 삭제 | 없음 | 해당 `label:{labelId}` 삭제 |

    * 친밀도 업데이트 및 Decay 배치: 무효화하지 않음. 10분 TTL 자연 만료에 위임.