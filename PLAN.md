# PLAN: Task-57 — FlagDetailResult에 isHost 필드 추가

## 목표

`GET /api/v1/flags/{id}` 응답에 요청자가 호스트인지 나타내는 `isHost: boolean` 필드 추가.
프론트엔드에서 수정/삭제/초대권한 변경 UI를 조건부 노출하는 데 사용한다.

---

## 변경 파일 (4개)

### 1. `FlagDetailResult.java`

`isHost` 필드 추가 및 `of()` 팩토리 메서드 시그니처 변경.

```java
public record FlagDetailResult(
    Long id, String title, String description,
    int capacity, int participantCount,
    Long parentFlagId, String status,
    FlagScheduleResult schedule,
    FlagHostResult host,
    ParentFlagResult parentFlag,
    List<ParticipantResult> participants,
    boolean isHost          // 추가
) {
    public static FlagDetailResult of(
            Flag flag, FlagUserInfo hostInfo,
            Flag parentFlag, List<ParticipantResult> participants,
            boolean isHost) { ... }
}
```

### 2. `FlagQueryUseCase.java`

`viewerId` 파라미터 추가.

```java
FlagDetailResult getFlagDetail(Long flagId, Long viewerId);
```

### 3. `FlagQueryService.java`

`viewerId`를 받아 `flag.getHostId().equals(viewerId)`로 계산 후 `of()`에 전달.

```java
public FlagDetailResult getFlagDetail(Long flagId, Long viewerId) {
    ...
    boolean isHost = flag.getHostId().equals(viewerId);
    return FlagDetailResult.of(flag, hostInfo, parentFlag, participants, isHost);
}
```

### 4. `FlagQueryController.java`

`@CurrentUserId` 추가하여 `viewerId` 전달.

```java
@GetMapping("/{id}")
public ResponseEntity<FlagDetailResult> getFlagDetail(
        @PathVariable Long id,
        @CurrentUserId Long currentUserId) {
    return ResponseEntity.ok(flagQueryUseCase.getFlagDetail(id, currentUserId));
}
```

---

## 영향 범위

### Production 코드
- `FlagDetailResult.of()` 시그니처 변경 → `FlagQueryService` 내부 호출부만 수정
- `FlagQueryUseCase.getFlagDetail()` 시그니처 변경 → `FlagQueryController` 호출부만 수정

### 테스트 코드
| 파일 | 작업 |
|------|------|
| `FlagQueryServiceTest` | 기존 `getFlagDetail(flagId)` 호출 5곳을 `getFlagDetail(flagId, viewerId)` 형태로 수정 + `isHost=true/false` 케이스 신규 추가 |
| `FlagControllerTest` | 수정 불필요 — `getFlagDetail` 관련 테스트 없음 |
| `FlagQueryControllerTest` | 파일 없음 — 이번 Task 범위 제외 (신규 Controller 테스트는 별도 Task로) |

> **주의**: `FlagControllerTest`는 기존 PLAN에 잘못 포함됐던 항목. `getFlagDetail` 관련 테스트를 포함하지 않아 수정 불필요.

## 브랜치
`ai/feat-flag-detail-is-host` (from main)
