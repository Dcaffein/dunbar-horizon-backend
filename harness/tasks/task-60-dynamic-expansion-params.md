# Task-60: 앵커 확장 파라미터 동적화

## Background

현재 두 앵커 확장 엔드포인트의 `limit`, `threshold` 파라미터가 하드코딩되어 있다.
서비스 레이어에 이미 `expansionValue(0.0~1.0)` 기반의 구간별 동적 계산 로직이 존재하지만 실제로 활용되지 않는 dead code 상태다.

## Objective

### 1. `GET /api/v1/networks/suggestions/anchor?anchorId=...`

클라이언트가 `expansionValue` 파라미터를 직접 전달해 탐색 범위를 제어할 수 있도록 변경.

- `@RequestParam Double expansionValue` 추가
- 기존 `getAnchorExpansion(userId, anchorId, expansionValue)` 서비스 메서드로 연결
- `limit`, `threshold`는 서비스의 기존 구간별 계산 로직이 처리

### 2. `GET /api/v1/networks/recommendations?anchorId=...`

나와 앵커 사이의 `Friendship.intimacy`를 기반으로 `limit`, `threshold`를 동적으로 결정.

- intimacy 값을 조회해 `expansionValue`로 활용하거나 별도 계산 로직 적용
- 현재 하드코딩된 `0.3` 제거

## Current State

```java
// suggestions/anchor: threshold=1, limit=10 고정
getTwoHopSuggestionsByOneHop(userId, anchorId)
→ getRecommendedNetworkByAnchor(userId, anchorId, threshold=1, limit=10)

// recommendations: expansionValue=0.3 고정
getAnchorExpansion(userId, anchorId, 0.3)
→ limit=25, threshold=4 고정
```

## Open Questions

- recommendations 엔드포인트에서 intimacy 조회를 서비스 레이어에서 할지, 컨트롤러에서 할지
- intimacy 범위(0.0~1.0)가 기존 `expansionValue` 입력 범위와 동일한지 확인 필요

## Decision

미정. PLAN.md에서 구체화 예정.
