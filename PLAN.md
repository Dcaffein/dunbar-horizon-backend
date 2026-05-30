# PLAN: GET /api/v1/flags/{id} 상세 조회 엔드포인트 추가

## 목표
- `FlagResult`에 `parentFlagId` 추가 (목록에서 encore 여부 노출)
- `GET /api/v1/flags/{id}` 엔드포인트 신규 추가 (접근 제한 없음)
- 응답 DTO `FlagDetailResult`를 flat 구조로 재설계: FlagResult 필드 + `parentFlag?: { id, title }` + `participants`

---

## 현황 분석

| 항목 | 현재 상태 |
|------|---------|
| `Flag.parentId` | 존재 (`@Getter`) |
| `FlagQueryUseCase.getFlagDetail()` | 구현 완료, 단 host/participant 접근 제한 있음 |
| `FlagDetailResult` | `FlagResult`를 nested로 감싼 구조 + `role` 포함 |
| `GET /api/v1/flags/{id}` | 컨트롤러 엔드포인트 없음 |

---

## 변경 파일 (7개)

### 1. `FlagResult.java` — `parentFlagId` 추가
```java
public record FlagResult(
    Long id,
    String title,
    String description,
    int capacity,
    int participantCount,
    @Nullable Long parentFlagId,   // 추가
    String status,
    FlagScheduleResult schedule,
    FlagHostResult host
) {
    public static FlagResult of(Flag flag, FlagUserInfo hostInfo, int participantCount) { ... }
}
```
`flag.getParentId()` 값을 `of()` 내부에서 직접 읽으므로 시그니처 변경 없음.

### 2. `ParticipantResult.java` — 필드 정리
`userId` → `id`, `joinedAt` 제거 (스펙에서 불필요)
```java
public record ParticipantResult(Long id, String nickname, String profileImageUrl) {
    public static ParticipantResult of(FlagUserInfo userInfo) { ... }
}
```

### 3. `ParentFlagResult.java` — 신규 DTO 생성
```java
public record ParentFlagResult(Long id, String title) {
    public static ParentFlagResult from(Flag flag) { ... }
}
```

### 4. `FlagDetailResult.java` — flat 구조로 재설계
nested `FlagResult` 제거, FlagResult 필드를 직접 포함. `role` 제거.
```java
public record FlagDetailResult(
    Long id,
    String title,
    String description,
    int capacity,
    int participantCount,
    @Nullable Long parentFlagId,
    String status,
    FlagScheduleResult schedule,
    FlagHostResult host,
    @Nullable ParentFlagResult parentFlag,
    List<ParticipantResult> participants
) {
    public static FlagDetailResult of(Flag flag, FlagUserInfo hostInfo,
                                      @Nullable Flag parentFlag,
                                      List<ParticipantResult> participants) { ... }
}
```

### 5. `FlagQueryUseCase.java` — 시그니처 변경
`viewerId` 제거 (접근 제한 없음):
```java
FlagDetailResult getFlagDetail(Long flagId);
```

### 6. `FlagQueryService.java` — getFlagDetail 재구현
- `validateAccess` 호출 제거
- `role` 계산 제거
- `parentId`가 있으면 `flagRepository.findById(parentId)` 호출
- 새 flat `FlagDetailResult` 빌드

### 7. `FlagQueryController.java` — 엔드포인트 추가
```java
@GetMapping("/{id}")
public ResponseEntity<FlagDetailResult> getFlagDetail(@PathVariable Long id) {
    return ResponseEntity.ok(flagQueryUseCase.getFlagDetail(id));
}
```
`@CurrentUserId` 불필요 (인증은 Security filter에서 처리, 서비스 레이어에 ID 전달 안 함)

---

## 영향 범위
- `ParticipantResult.of()` 시그니처 변경 → `FlagQueryService.getFlagDetail()` 내부 호출부만 수정
- 기존 `getFlagDetail(flagId, viewerId)` 호출부는 컨트롤러에 없으므로 컴파일 오류 없음
- `FlagResult.of()` 시그니처 변경 없음 — `parentFlagId`는 `Flag.getParentId()`에서 내부 읽기

## 브랜치
`ai/feat-flag-detail-endpoint` (from main)
