# PLAN: Task-60 앵커 확장 파라미터 동적화

## 목표

- `GET /api/v1/networks/suggestions/anchor` — 클라이언트가 전달한 `expansionValue`로 limit/threshold 동적 결정 (excludeMyFriends=true)
- `GET /api/v1/networks/recommendations` — 나와 앵커 사이의 `Friendship.intimacy`를 expansionValue로 사용해 limit/threshold 동적 결정 (excludeMyFriends=false)

---

## 현황

| 엔드포인트 | 현재 파라미터 | 문제 |
|------------|--------------|------|
| `/suggestions/anchor` | threshold=1, limit=10 고정 | 클라이언트 제어 불가 |
| `/recommendations` | expansionValue=0.3 하드코딩 | 관계 친밀도 미반영, calculateLimit/Threshold dead code |

---

## 변경 파일 (3개)

### 1. `SocialExpansionQueryUseCase.java` (port/in) — 시그니처 변경

```java
// 변경 전
List<AnchorExpansionResult> getTwoHopSuggestionsByOneHop(Long userId, Long anchorId);
List<AnchorExpansionResult> getAnchorExpansion(Long userId, Long anchorFriendId, Double expansionValue);

// 변경 후
List<AnchorExpansionResult> getTwoHopSuggestionsByOneHop(Long userId, Long anchorId, Double expansionValue);
List<AnchorExpansionResult> getAnchorExpansion(Long userId, Long anchorFriendId);
```

### 2. `SocialExpansionQueryService.java` — 서비스 로직 변경

**getTwoHopSuggestionsByOneHop**: expansionValue 파라미터 추가, 기존 validateExpansionValue / calculateLimit / calculateThreshold 활용

```java
public List<AnchorExpansionResult> getTwoHopSuggestionsByOneHop(Long userId, Long anchorId, Double expansionValue) {
    validateExpansionValue(expansionValue);
    int limit = calculateLimit(expansionValue);
    int threshold = calculateThreshold(expansionValue);
    return socialExpansionRepository.getRecommendedNetworkByAnchor(userId, anchorId, threshold, limit);
}
```

**getAnchorExpansion**: expansionValue 파라미터 제거, `FriendshipRepository`에서 intimacy 조회 후 내부에서 계산

```java
public List<AnchorExpansionResult> getAnchorExpansion(Long userId, Long anchorId) {
    double intimacy = friendshipRepository
            .findById(Friendship.generateCompositeId(userId, anchorId))
            .orElseThrow(() -> new FriendshipNotFoundException(userId, anchorId))
            .getIntimacy();
    int limit = calculateLimit(intimacy);
    int threshold = calculateThreshold(intimacy);
    return socialExpansionRepository.getRelatedNetworkByAnchor(userId, anchorId, threshold, limit);
}
```

intimacy 범위: `Friendship.calculateIntimacyPolicy = Math.sqrt(scoreA * scoreB)`, normalizedScore가 [0,1]이므로 intimacy도 [0,1] — validateExpansionValue 범위와 일치. `SocialExpansionQueryService`에 `FriendshipRepository` 의존성 추가 필요.

### 3. `SocialQueryController.java` — 엔드포인트 변경

```java
// /recommendations: expansionValue 파라미터 제거
@GetMapping("/recommendations")
public ResponseEntity<List<AnchorExpansionResult>> getTwoHopRecommendation(
        @CurrentUserId Long currentUserId,
        @RequestParam Long anchorId
) {
    return ResponseEntity.ok(expansionQueryUseCase.getAnchorExpansion(currentUserId, anchorId));
}

// /suggestions/anchor: expansionValue 파라미터 추가
@GetMapping("/suggestions/anchor")
public ResponseEntity<List<AnchorExpansionResult>> getTwoHopSuggestionsByAnchor(
        @CurrentUserId Long currentUserId,
        @RequestParam Long anchorId,
        @RequestParam Double expansionValue
) {
    return ResponseEntity.ok(expansionQueryUseCase.getTwoHopSuggestionsByOneHop(currentUserId, anchorId, expansionValue));
}
```

---

## 영향 범위

- `SocialExpansionQueryUseCase` 시그니처 변경 → 컨트롤러 호출부 수정 필요 (위에서 처리)
- `getAnchorExpansion(userId, anchorId, 0.3)` 호출부는 컨트롤러 1곳뿐 — 컴파일 오류 없음
- `SocialExpansionRepository` (port/out) 변경 없음 — 신규 메서드 불필요
- 기존 `calculateLimit` / `calculateThreshold` / `validateExpansionValue` 로직 변경 없음
- `SocialExpansionQueryService`에 `FriendshipRepository` 의존성 추가

## 브랜치

`ai/feat-dynamic-expansion-params` (from main)
