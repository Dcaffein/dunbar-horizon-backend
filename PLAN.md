# PLAN: Social 네트워크 조회 Redis 캐시 도입 — 아키텍처 리팩토링 (task-23)

## 작업 목표
`getDefaultIntimacyNetwork`, `getLabelCustomNetwork`에 `@Cacheable` AOP 기반 캐시를 도입하되,
서비스와 이벤트 리스너가 포트(추상화)만 바라보는 헥사고날 구조를 유지한다.

---

## [도메인 수정 승인 요청]
- `social/domain/label/event/LabelMemberChangedEvent.java` 
- `LabelService` 이벤트 발행 

---

## 계층 구조

```
Application layer
  SocialNetworkQueryService         → SocialNetworkRepository    (쿼리 포트)
  SocialNetworkCacheEvictListener   → SocialNetworkCacheManager  (무효화 포트)

Adapter/out layer
  SocialNetworkRepositoryAdapter    (implements SocialNetworkRepository)
    @Cacheable → AOP이 Redis Look-Aside 처리
    Neo4jClient → 캐시 miss 시 실제 조회

  SocialNetworkCacheAdapter         (implements SocialNetworkCacheManager)
    RedisTemplate → 패턴 기반 키 삭제 (eviction 전용)
```

---

## 포트 설계

### SocialNetworkRepository (쿼리 포트 — 서비스가 참조)
```java
List<NetworkFriendEdgeResult> getDefaultIntimacyNetwork(Long userId, DunbarCircle circleSize);
List<NetworkFriendEdgeResult> getLabelCustomNetwork(Long userId, String labelId);
List<MutualFriendEdgeResult> getIntersectionByOneHop(Long userId, Long targetId, String labelName, int limitSize);
List<NetworkOneHopsByTwoHopResult> getIntersectionOneHops(Long userId, Long targetId, String labelName, int limitSize);
```

### SocialNetworkCacheManager (무효화 포트 — 이벤트 리스너가 참조)
```java
void evictDefaultNetwork(Long userId);
void evictLabelNetwork(Long userId, String labelId);
void evictAllLabelNetworks(Long userId);
```

---

## @Cacheable 적용 (SocialNetworkRepositoryAdapter)

```java
@Cacheable(cacheNames = "dunbar:network:default", key = "#userId + ':' + #circleSize.name()")
public List<NetworkFriendEdgeResult> getDefaultIntimacyNetwork(Long userId, DunbarCircle circleSize) {
    // Neo4j 조회만 작성 — Look-Aside는 AOP가 처리
}

@Cacheable(cacheNames = "dunbar:network:label", key = "#userId + ':' + #labelId")
public List<NetworkFriendEdgeResult> getLabelCustomNetwork(Long userId, String labelId) {
    // Neo4j 조회만 작성 (limitSize = DUNBAR.getLimitSize() 고정)
}
```

실제 Redis 키:
```
dunbar:network:default:{userId}:{circleSize}
dunbar:network:label:{userId}:{labelId}
```

---

## RedisConfig — RedisCacheManager 추가

```java
@Bean
public CacheManager cacheManager(RedisConnectionFactory cf) {
    RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(10))
        .computePrefixWith(cacheName -> cacheName + ":")   // "::" → ":" 구분자 정리
        .serializeValuesWith(
            RedisSerializationContext.SerializationPair.fromSerializer(
                new GenericJackson2JsonRedisSerializer(objectMapper)
            )
        );
    return RedisCacheManager.builder(cf).cacheDefaults(config).build();
}
```

---

## 무효화 정책 (SocialNetworkCacheEvictListener — AFTER_COMMIT)

| 이벤트 | Default Network | Label Network |
|---|---|---|
| 친구 추가 | 양측 `evictDefaultNetwork` | 없음 |
| 친구 삭제 | 양측 `evictDefaultNetwork` | 양측 `evictAllLabelNetworks` |
| 라벨 멤버 추가 | 없음 | `evictLabelNetwork(ownerId, labelId)` |
| 라벨 멤버 삭제 | 없음 | `evictLabelNetwork(ownerId, labelId)` |

`evictAllLabelNetworks`는 `RedisTemplate.keys("dunbar:network:label:{userId}:*")` 패턴 삭제로 구현.
`@CacheEvict`은 단일 키만 지원하므로 패턴 삭제에는 `RedisTemplate` 직접 사용.

---

## 변경 파일 목록

### 삭제 (이전 커밋에서 잘못 생성된 파일)
| 파일 |
|---|
| `social/adapter/out/cache/SocialNetworkCacheAdapter.java` |
| `social/application/port/out/SocialNetworkCacheRepository.java` |

### 신규 생성
| 파일 | 역할 |
|---|---|
| `social/application/port/out/SocialNetworkCacheManager.java` | 무효화 전용 포트 |
| `social/adapter/out/SocialNetworkCacheAdapter.java` | `SocialNetworkCacheManager` 구현, RedisTemplate 사용 |

### 수정
| 파일 | 변경 내용 |
|---|---|
| `global/config/RedisConfig.java` | `RedisCacheManager` Bean 추가 |
| `SocialNetworkNeo4jRepositoryAdapter.java` → `SocialNetworkRepositoryAdapter.java` | `@Cacheable` 추가, `DunbarCircle` 파라미터 적용 |
| `SocialNetworkRepository.java` (포트) | `getDefaultIntimacyNetwork` 파라미터 `int` → `DunbarCircle`, `getLabelCustomNetwork` `limitSize` 제거 |
| `SocialNetworkQueryService.java` | `SocialNetworkCacheRepository` 의존 제거, 단일 포트만 참조 |
| `SocialNetworkCacheEvictListener.java` | `SocialNetworkCacheRepository` → `SocialNetworkCacheManager` 로 교체 |
