# Task-55: SocialNetworkQueryService @Neo4jTransactional 제거 및 어댑터 트랜잭션 어노테이션 정리

## Background

부하 테스트(200 VU) 중 VisualVM 프로파일링에서 캐시 히트 요청에서도
`NetworkSession.beginTransactionAsync()`, `TransactionInterceptor`가 반복 호출되는 것을 확인했다.

`@Neo4jTransactional`이 서비스 클래스 레벨에 선언되어 있어, `@Cacheable`이 Redis 히트를
반환하더라도 Neo4j 트랜잭션 BEGIN/COMMIT 왕복이 매 요청마다 발생하고 있었다.

추가로 `SocialNetworkRepositoryAdapter`, `SocialExpansionRepositoryAdapter`에
`@Transactional(readOnly = true)`(Spring 기본 = JPA 트랜잭션 매니저)가 붙어 있으나
두 어댑터는 `Neo4jClient`를 직접 주입받아 사용하므로 JPA 트랜잭션과 무관한 버그였다.

## Objective

- `SocialNetworkQueryService`에서 `@Neo4jTransactional` 제거
- `SocialNetworkRepositoryAdapter`, `SocialExpansionRepositoryAdapter`에서
  `@Transactional(readOnly = true)` 제거

## Decision

`Neo4jClient`는 트랜잭션 어노테이션 없이 auto-commit 모드로 읽기 쿼리를 실행할 수 있다.
`@Cacheable` 메서드는 캐시 히트 시 Neo4j에 도달하지 않으며,
캐시 미스 시에도 읽기 전용 단건 쿼리이므로 명시적 트랜잭션이 불필요하다.

서비스는 포트만 알고 인프라를 모른다는 헥사고날 원칙상,
`@Neo4jTransactional`은 서비스가 아닌 어댑터 관심사이며,
`Neo4jClient` 직접 사용 어댑터에서는 해당 어노테이션 자체가 불필요하다.

## Result

@Neo4jTransactional 제거 후 200 VU 부하 테스트 재실행 결과 예측이 정확히 맞았다.

**Phase 4 → Phase 5 비교 (200 VU, `results/load_phase4_2026-06-01_2110.json`)**

| 지표 | Phase 4 (제거 전) | Phase 5 (제거 후) | 개선 |
|------|------------------:|------------------:|-----:|
| Throughput | 481 req/s | **2,394 req/s** | +397% |
| avg | 230ms | **5.9ms** | -97% |
| p(95) | 402ms | **12.6ms** | -97% |

캐시 히트율이 ~100%인 상태에서 @Neo4jTransactional이 매 요청마다
Neo4j BEGIN/COMMIT 왕복(각 ~15ms)을 강제하고 있었고, 이것이 200ms 이상의 잔여 지연 원인이었다.
제거 후 캐시 히트 경로는 Redis 조회 + Jackson 역직렬화만 남아 순수 Redis 응답 속도에 도달했다.
