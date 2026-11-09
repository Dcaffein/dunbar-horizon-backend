# Task-133: 기본 관계망 캐시를 걷어 낸다

## Background

기본 관계망 조회(`SocialNetworkRepositoryAdapter.getDefaultNetworkGraph`)는 150명 서클(`DunbarCircle.DUNBAR`)일 때 Redis에 10분 동안 캐시된다. 무효화는 친구 관계가 삭제될 때(`FriendShipDeletedEvent`)만 `SocialNetworkCacheEvictListener`가 한다.

캐시는 HEX 모델에서 이 조회가 동시 요청 아래 Neo4j CPU를 거의 다 쓰며 처리량이 멈췄기 때문에 넣었다. task-128로 친구 관계가 단일 `FRIENDS_WITH` 관계가 된 뒤, 같은 데이터(사용자 1만 명, 친구 쌍 약 107만)와 같은 조건에서 다시 쟀다(2026-11-09, Neo4j Desktop 2026.10.1, page cache 512MB, 결과는 `Neo4jOptimizeTest/profiling/results/case5-model/pc512`). 서버 응답 p50은 71ms에서 35ms, p99는 116ms에서 59ms, 처리량 상한은 초당 약 60건에서 약 125건이 됐다.

캐시가 남아 있으면 신선도 문제가 따라온다. 친구 삭제 외의 변화(관심도·친밀도 변화, 새 친구, 그 밖의 가시성 변화)는 최대 10분 동안 관계망에 반영되지 않는다. 가시성 변화 쪽의 실제 영향은 확인하지 않았다.

## Objective

기본 관계망은 항상 Neo4j에서 조회한다. 관계망 캐시와 그 무효화 경로가 코드에서 사라지고, 관계망에 신선도 지연이 없어진다.

## Decision

### 캐시 없이 조회한다

단일 연결 모델에서는 캐시가 필요할 만큼 느리지 않고, 캐시를 두면 무효화 범위를 계속 관리해야 한다. 포트폴리오 사례 5("중간 노드를 없앤 뒤에는 캐시가 필요할 정도의 성능이 아니라고 판단했다")가 이 판단에 기대고 있다.

### 걷어 낼 범위

`@Cacheable`, 캐시 무효화 리스너, `SocialNetworkCacheManager` 포트와 그 Redis 어댑터, `dunbar:network:default` 캐시 설정을 걷어 내고 각각의 테스트도 정리한다.

### Redis는 남긴다

Redis는 가입 대기(`PendingSignupRedisAdapter`)와 스케줄러 잠금(`RedisLockProvider`)에도 쓴다. 연결과 `RedisTemplate`은 그대로 둔다. `CacheManager` 빈과 `@EnableCaching`은 다른 캐시가 남아 있지 않은지 확인한 뒤 함께 걷어 낸다.

운영 Redis에 남은 관계망 캐시 키는 TTL 10분으로 저절로 사라지므로 따로 지우지 않는다.

## Out of Scope

- Redis 자체 제거
- 관계망 Cypher 쿼리 변경
- 라벨 관계망과 2-hop 연결 조회

## Acceptance Criteria

- 관계망 조회에 캐시가 걸려 있지 않고, 캐시 무효화 리스너·포트·어댑터·캐시 설정이 남아 있지 않다.
- 지운 클래스의 테스트가 함께 정리되고, 관계망 조회 테스트가 통과한다.
- 가입 대기와 스케줄러 잠금의 Redis 사용이 그대로 동작한다.
- 애플리케이션 컨텍스트가 뜬다.
