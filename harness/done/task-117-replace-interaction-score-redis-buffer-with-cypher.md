# Task-117: Redis 관심도 버퍼 제거 및 Cypher 점수 갱신 경로 재구성

## Background

기존 점수 갱신은 상호작용 하나마다 `findById → Friendship 도메인 로직 → save`를 수행했다. 이 경로는 Neo4j read/write 왕복, aggregate 매핑, SDN 관계 동기화를 매 이벤트마다 수행한다.

Task-28은 이 비용을 줄이기 위해 Redis delta buffer와 주기 flush를 도입했다. 그러나 Redis가 Neo4j write를 크게 줄이려면 같은 Friendship에 대한 이벤트가 짧은 시간에 반복적으로 몰려야 한다. 일반 상호작용은 Friendship을 구성하는 두 사용자의 행동에서 비롯되므로, 특정 Friendship 하나에 대량 이벤트가 집중될 가능성이 낮다.

후속 benchmark는 이 서비스 특성을 모델링하기 위해 20초 동안 Friendship당 약 2~3건의 이벤트가 발생하도록 구성했다. 이는 실제 트래픽 관측값이 아니라 설계 가정이다. 그 조건에서 직접 Cypher 갱신은 목표 부하를 감당했고, Redis buffer의 병합 이득은 반영 지연, key drain, 실패 경계, 운영 복잡도를 정당화할 만큼 확인되지 않았다.

따라서 일반 상호작용은 Redis에 누적하지 않고 Cypher로 즉시 갱신한다.

다만 Flag 종료는 다른 성격의 작업이다. 참가자가 `n`명일 때 host-참가자 `n`개와 참가자 쌍 `n(n-1)/2`개, 최대 `n(n+1)/2`개의 Friendship score 갱신이 필요하다. 이들을 관계마다 개별 Neo4j transaction으로 보내면 요청과 transaction 수가 과도해진다. Flag 하나는 Friendship 관계들을 하나의 `UNWIND` Cypher batch로 갱신해야 한다.

여러 Flag 종료가 동시에 실행되면 문제가 생긴다. 각 batch transaction은 갱신한 관계의 write lock을 commit까지 유지한다. 서로 다른 Flag batch가 일부 Friendship을 공통으로 포함하고 다른 순서로 lock을 얻으면 deadlock이 발생할 수 있다.

```text
Flag batch A: Friendship X lock → Friendship Y lock 대기
Flag batch B: Friendship Y lock → Friendship X lock 대기
```

`FriendshipDecayService`도 여러 Friendship의 `interestScore`와 `intimacy`를 갱신하므로 같은 종류의 multi-Friendship writer다.

또한 `interestScore`는 `SET r.interestScore = r.interestScore + $delta`로 원자적으로 누적할 수 있지만, `intimacy`는 양 방향 `HAS_FRIENDSHIP` 관계의 score를 읽어 계산한 파생값이다. 따라서 모든 점수 갱신 경로는 score를 읽기 전에 공통 `Friendship` write lock을 얻고, score 갱신과 intimacy 재계산을 같은 Neo4j transaction에서 끝내야 한다.

관계 엔티티의 기술 식별자는 선행 작업에서 `String`에서 `Long`으로 전환됐다. 이는 `@Id @GeneratedValue` 관계가 저장 프로퍼티가 아니라 Neo4j native relationship ID를 SDN과 일관되게 매핑하도록 한 수정이다. 이번 작업에서 이 변경을 되돌리거나 별도 데이터 마이그레이션하지 않는다.

## Objective

Redis interaction-score buffer를 제거한다.

- 일반 상호작용은 Friendship 단위 Cypher로 즉시 반영한다.
- Flag 종료와 decay처럼 여러 Friendship을 갱신하는 작업은 `IntimacyScoreManager`를 통해 하나씩 실행한다.
- 모든 갱신 경로는 Friendship 공통 lock 아래 interestScore와 intimacy를 한 transaction에서 갱신한다.

다음 도메인 결과는 유지한다.

- 단방향 상호작용은 이벤트 주체 방향의 `interestScore`만 양의 delta만큼 갱신한다.
- 상호 상호작용은 양쪽 방향의 `interestScore`를 양의 delta만큼 갱신한다.
- score 감소는 이 경로가 아니라 별도 decay에서 `0 < rate < 1` 배율을 곱해 수행한다.
- `intimacy`는 갱신 뒤 양쪽 관심도를 `score / (score + 50.0)`으로 정규화한 기하 평균이다.
- 일반 상호작용의 `lastInteractedAt`은 즉시 Cypher 반영 시각으로 갱신한다.

## Domain Change

[ ] 없음  [x] 있음

- 관심도·친밀도 쓰기의 실행 정본은 aggregate load/save에서 Cypher 갱신으로 이동한다.
- `Friendship`과 `FriendRecognition`의 계산 메서드는 삭제하지 않는다. 정책의 참조 구현 및 Cypher 결과 동등성 검증 기준으로 유지한다.
- 일반 상호작용의 점수 반영은 Redis flush 지연 없이 즉시 durable 상태가 된다.
- Flag/decay 점수 반영은 순차 manager의 처리 대기 시간만큼 지연될 수 있다.

## Decision

### Redis buffer를 제거한다

Redis는 같은 Friendship delta가 반복될 때만 Neo4j write를 병합한다. 현재 서비스의 일반 상호작용 특성에서는 이 병합 효과가 불확실하고, Redis drain과 Neo4j 반영 사이의 유실 경계 및 최대 flush 지연을 추가한다.

이번 작업은 Redis 전체를 제거하지 않는다. 네트워크 조회 캐시, 이메일 검증 등 다른 Redis 사용처에는 영향을 주지 않는다.

### 일반 상호작용은 즉시 Cypher로 처리한다

`FriendInteractionEventListener`는 Redis delta port 대신 Friendship score 갱신 port를 호출한다. Cypher는 aggregate를 읽거나 SDN `save()`를 호출하지 않고 현재 relationship property에 직접 delta를 적용한다.

단순 score delta는 relationship property의 직접 의존 update로 원자적으로 누적한다. 하지만 intimacy는 두 관계 score의 파생값이므로, query는 score를 읽기 전에 Friendship node의 dummy property write (`SET → REMOVE`)로 공통 write lock을 획득해야 한다. lock은 transaction 종료까지 유지되며, 그 뒤 양쪽 score를 갱신하고 intimacy를 재계산한다.

### Flag와 decay는 IntimacyScoreManager가 직렬화한다

`IntimacyScoreManager`는 multi-Friendship 작업을 전용 단일 worker queue에서 실행한다.

- Flag 하나는 참가자 관계 전체를 하나의 `UNWIND` Cypher batch transaction으로 반영한다.
- 참가자 목록은 유일하고 host를 중복 포함하지 않는다는 도메인 전제 아래, 한 Flag 내부의 Friendship은 중복되지 않는다. 불필요한 Friendship별 delta 병합을 추가하지 않는다.
- manager는 호출자를 block하지 않고 작업을 queue에 넣으며, 한 Flag의 batch가 commit 또는 rollback된 뒤 다음 작업을 실행한다.
- decay도 manager를 통해 실행하므로 Flag batch와 동시에 실행되지 않는다.
- 일반 단건 상호작용은 manager를 거치지 않는다. 대신 같은 Friendship 공통 lock 규칙을 사용해 Flag/decay와 만날 때 score와 intimacy의 정합성을 보장한다.

이 직렬화는 대형 batch끼리의 deadlock을 정상 흐름에서 제거한다. 제한된 재시도는 예상하지 못한 transient failure의 안전망일 뿐, deadlock을 정상 처리 방식으로 허용하지 않는다.

### 자동 Flag 만료는 오래된 100개씩 처리한다

만료 scheduler는 매시간 종료 시각이 오래된 Flag부터 최대 100개를 선택해 soft delete한다. 각 Flag의 종료 사실은 score worker queue에 순서대로 들어가며, Flag 내부의 모든 Friendship pair는 하나의 transaction으로 처리한다.

### 실패는 best-effort로 관측·복구한다

관심도·친밀도는 권한·금액·가입·삭제 상태 같은 원천 데이터가 아니라, 지연 및 사후 보정이 가능한 행동 기반 파생 지표다.

Flag batch가 rollback된 것이 확인된 transient failure에만 제한된 재시도를 수행한다. commit 성공 여부가 불명확한 실패와 반복 실패는 `flagId`, 참가자 수, pair 수, 예외를 남겨 운영자가 사후 재실행 또는 보정할 수 있게 한다. 이 선택은 프로세스 장애 시 일부 점수 갱신이 누락될 수 있음을 명시적으로 수용한다.

## Scope

1. `FriendInteractionEventListener`의 Redis 누적 경로를 일반 단건 Cypher 점수 갱신 port 호출로 바꾼다.
2. Friendship persistence port와 Neo4j adapter에 단방향·상호 score delta 및 intimacy 재계산을 수행하는 Cypher 갱신을 구현한다.
3. 모든 점수 mutation Cypher가 Friendship dummy-property lock을 score read 이전에 획득하도록 통일한다.
4. Flag 종료의 Friendship pair를 하나의 `UNWIND` Cypher batch로 갱신하는 use case를 구현한다.
5. Flag batch와 decay를 전용 단일 worker queue에서 하나씩 실행하는 `IntimacyScoreManager`를 구현한다.
6. Flag 자동 만료가 매시간 오래된 Flag 최대 100개를 soft delete하고, Flag별 점수 갱신을 queue에 넣도록 변경한다.
7. Redis interaction-score buffer 전용 port, adapter, flush service, flush scheduler 및 테스트를 제거한다.
8. aggregate 메서드로 계산한 기대값과 Cypher 갱신 뒤 실제 그래프 값이 동일함을 검증하는 테스트를 추가한다.
9. Flag batch 실패의 제한된 재시도와 실패 맥락 로그를 추가한다.

## Out of Scope

- Flag 종료의 `O(n²)` pair 생성 정책 변경
- 한 Flag를 여러 Neo4j transaction chunk로 분할하는 정책
- Flag 참여자 수 상한의 정책 변경
- 관심도 delta, 정규화 상수, 친밀도 정책의 변경
- 네트워크 조회 캐시 등 다른 Redis 활용의 제거
- 이미 완료된 관계 ID String → Long 전환 및 Neo4j 데이터 마이그레이션
- 이 작업 구현 중 부하 테스트 수행 및 `Neo4jOptimizeTest` 결과 갱신

## Acceptance Criteria

- 단방향·상호 상호작용 모두 aggregate 참조 구현과 같은 score/intimacy 결과를 낸다.
- Cypher query는 Friendship aggregate 조회나 SDN `save()`를 호출하지 않는다.
- 모든 점수 mutation query는 Friendship 공통 lock을 score read 이전에 획득하고, score 갱신과 intimacy 재계산을 같은 transaction에서 수행한다.
- 같은 Friendship에 대한 병렬 단건 갱신 후 interestScore는 모든 delta가 누적되고 intimacy는 최종 score를 기반으로 계산된다.
- Flag 하나는 참가자 전체 Friendship을 개별 Neo4j 요청이 아닌 하나의 Cypher batch transaction으로 반영한다.
- 여러 Flag 종료가 들어와도 multi-Friendship Cypher batch는 동시에 두 개 이상 실행되지 않는다.
- decay와 Flag batch는 동시에 실행되지 않는다.
- 자동 Flag 만료는 매시간 종료 시각이 오래된 Flag 최대 100개만 soft delete하고, 각 Flag의 점수 갱신을 queue에 넣는다.
- Flag batch의 반복 실패는 식별 가능한 맥락과 함께 로그로 남는다.
- interaction-score Redis buffer/flush bean과 scheduler가 프로덕션 애플리케이션 컨텍스트에 남지 않는다.
