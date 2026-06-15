# task-77: Redis KEYS → SCAN 교체 (라벨 캐시 전체 무효화)

## 배경

`SocialNetworkCacheAdapter.evictAllLabelNetworks()`는 친구 관계가 끊길 때 해당 유저의
모든 라벨 네트워크 캐시를 일괄 삭제한다. 현재 `redisTemplate.keys()` 명령어를 사용 중인데,
`KEYS`는 Redis를 O(N) 블로킹하는 위험한 명령어다. 유저/키가 많아질수록 Redis 전체가
해당 시간 동안 응답하지 못한다.

## 문제

```java
// SocialNetworkCacheAdapter.java
Set<String> keys = redisTemplate.keys("dunbar:network:label:" + userId + ":*");
```

`KEYS`는 Redis 싱글 스레드를 점유하며 전체 키 공간을 순회한다.
서비스 규모가 커지면 친구 삭제 한 번에 Redis가 수백ms 동안 멈출 수 있다.

## 해결 방향

`redisTemplate.keys()` → `redisTemplate.scan(ScanOptions)` 로 교체.
`SCAN`은 cursor 기반으로 키를 조금씩 순회하므로 블로킹이 없다.

```java
// 변경 후 (RedisCallback 활용)
try (Cursor<byte[]> cursor = redisTemplate.executeWithStickyConnection(
        connection -> connection.scan(ScanOptions.scanOptions()
                .match("dunbar:network:label:" + userId + ":*")
                .count(100)
                .build()))) {
    List<String> keys = new ArrayList<>();
    cursor.forEachRemaining(k -> keys.add(new String(k)));
    if (!keys.isEmpty()) {
        redisTemplate.delete(keys);
    }
}
```

## 수정 대상

- `social/adapter/out/redis/SocialNetworkCacheAdapter.java` — `evictAllLabelNetworks()` 메서드

## 우선순위

낮음 — 현재 서비스 규모에서는 당장 문제 없음. 유저 수가 늘어나기 전에 처리 권장.
