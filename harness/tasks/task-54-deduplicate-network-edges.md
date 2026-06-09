# Task-54: SocialNetwork 응답 중복 엣지 제거

## Background

`buildDynamicPruningNetwork`는 경계 내 150명 멤버를 순회하며 각자 기준으로 연결을 탐색한다.
두 멤버 A, B가 서로를 각자의 `dynamicLimit` 안에 포함하면 (A,B)와 (B,A)가 모두 반환되어
같은 Friendship을 두 행으로 표현하는 중복이 발생한다.

중복 여부는 양쪽의 `dynamicLimit` 크기에 따라 결정되므로 항상 발생하지는 않는다.
현재 프론트엔드가 중복을 방어적으로 처리하고 있으나, 데이터 정합성 책임은 백엔드에 있다.

## Objective

- 현재 문제: 중복 엣지가 Redis에 그대로 캐싱되어 실제 필요 용량의 최대 2배 저장,
  캐시 히트 시에도 2E rows가 역직렬화·전송됨.
- 목표: 앱 레이어에서 중복을 제거하여 Redis 저장량과 프론트 전송량을 절반으로 줄인다.

## Domain Change

없음. `social/adapter/out/` 계층만 변경.

## Decision

**Neo4j 쿼리 변경 없이 Java 레이어에서 처리한다.**

Neo4j 레벨에서 `collect()[0]` 그룹핑으로 제거하면 현재 스트리밍 반환 방식이
전체 rows 적재 후 반환 방식으로 바뀌어 Neo4j 메모리 부담이 증가하므로 채택하지 않는다.

```java
private static Predicate<NetworkFriendEdgeResult> deduplicateEdge() {
    Set<String> seen = new HashSet<>();
    return r -> {
        long lo = Math.min(r.friendAId(), r.friendBId());
        long hi = Math.max(r.friendAId(), r.friendBId());
        return seen.add(lo + ":" + hi);
    };
}
```

`getDefaultIntimacyNetwork`, `getLabelCustomNetwork` 각 메서드의
`.all().stream()` 체인에 `.filter(deduplicateEdge())` 추가.

`@Cacheable`이 dedup 후 E rows를 캐싱하므로 캐시 히트 요청은 추가 비용 없이 이득을 얻는다.
캐시 미스 시 추가 비용은 O(E) HashSet 패스로 수 ms 이하.

## Open Question

없음.
