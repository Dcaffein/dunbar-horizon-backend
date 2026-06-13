# PLAN — Task 72: 후기 조회 응답에 locked 필드 추가

## 작업 목표

`GET /api/v1/flags/{flagId}/memorials` 응답을 `{ memorials, locked }` 구조로 변경.
서비스 레이어에서 2단계 체크로 비즈니스 규칙을 명시적으로 표현하고,
기존 단일 복합쿼리(`findAllMemorialsIfMemorialized`)를 제거한다.

---

## 구현 계획

### 1. `MemorialListResult` 신규 (application/dto/result)

```java
public record MemorialListResult(
        List<MemorialResult> memorials,
        boolean locked
) {
    public static MemorialListResult empty() {
        return new MemorialListResult(List.of(), false);
    }

    public static MemorialListResult locked() {
        return new MemorialListResult(List.of(), true);
    }

    public static MemorialListResult of(List<MemorialResult> memorials) {
        return new MemorialListResult(memorials, false);
    }
}
```

### 2. `FlagMemorialRepository` 포트 수정

```java
// 추가
boolean existsByFlagIdAndWriterId(Long flagId, Long writerId);
List<FlagMemorial> findAllByFlagId(Long flagId);

// 제거
List<FlagMemorial> findAllMemorialsIfMemorialized(Long flagId, Long viewerId);
```

### 3. `FlagMemorialJpaRepository` 수정

Spring Data 메서드명 자동 생성:
```java
// 추가 (메서드명만으로 쿼리 자동 생성)
boolean existsByFlagIdAndWriterId(Long flagId, Long writerId);
List<FlagMemorial> findAllByFlagId(Long flagId);

// 제거
@Query("SELECT fm FROM FlagMemorial fm WHERE ...") findAllMemorialsIfMemorialized(...)
```

### 4. `FlagMemorialRepositoryAdapter` 수정

```java
// 추가
@Override
public boolean existsByFlagIdAndWriterId(Long flagId, Long writerId) {
    return jpaRepository.existsByFlagIdAndWriterId(flagId, writerId);
}

@Override
public List<FlagMemorial> findAllByFlagId(Long flagId) {
    return jpaRepository.findAllByFlagId(flagId);
}

// 제거
findAllMemorialsIfMemorialized(...) 위임 메서드
```

### 5. `FlagMemorialQueryUseCase` 반환 타입 변경

```java
// Before
List<MemorialResult> getMemorials(Long flagId, Long viewerId);

// After
MemorialListResult getMemorials(Long flagId, Long viewerId);
```

### 6. `FlagMemorialQueryService` 로직 재작성

```java
@Override
public MemorialListResult getMemorials(Long flagId, Long viewerId) {
    if (!memorialRepository.existsByFlagId(flagId)) {
        return MemorialListResult.empty();         // 후기 자체가 없음
    }
    if (!memorialRepository.existsByFlagIdAndWriterId(flagId, viewerId)) {
        return MemorialListResult.locked();        // 본인이 안 남겨서 못 봄
    }

    List<FlagMemorial> memorials = memorialRepository.findAllByFlagId(flagId);
    List<Long> writerIds = memorials.stream()
            .map(FlagMemorial::getWriterId).distinct().toList();
    Map<Long, FlagUserInfo> writerMap = flagUserPort.findUserInfosByIds(writerIds);

    return MemorialListResult.of(
            memorials.stream()
                    .map(m -> MemorialResult.of(m,
                            writerMap.getOrDefault(m.getWriterId(),
                                    new FlagUserInfo(m.getWriterId(), "알 수 없는 사용자", null))))
                    .toList()
    );
}
```

### 7. `FlagMemorialController` 응답 타입 변경

```java
// Before
public ResponseEntity<List<MemorialResult>> getMemorials(...)

// After
public ResponseEntity<MemorialListResult> getMemorials(...) {
    return ResponseEntity.ok(memorialQueryUseCase.getMemorials(flagId, currentUserId));
}
```

---

## 테스트 계획

### `FlagMemorialQueryServiceTest` (신규, Mockito)

| 케이스 | 검증 |
|--------|------|
| 후기 없음 | `locked=false`, `memorials=[]` |
| 후기 있으나 본인 미작성 | `locked=true`, `memorials=[]` |
| 본인 작성 완료 | `locked=false`, `memorials` 내용 포함 |

---

## 변경 파일 요약

| 파일 | 유형 |
|------|------|
| `flag/application/dto/result/MemorialListResult.java` | 신규 |
| `flag/domain/memorial/repository/FlagMemorialRepository.java` | 수정 |
| `flag/adapter/out/persistence/jpa/FlagMemorialJpaRepository.java` | 수정 |
| `flag/adapter/out/persistence/FlagMemorialRepositoryAdapter.java` | 수정 |
| `flag/application/port/in/FlagMemorialQueryUseCase.java` | 수정 |
| `flag/application/service/memorial/FlagMemorialQueryService.java` | 수정 |
| `flag/adapter/in/web/FlagMemorialController.java` | 수정 |
| `flag/application/service/memorial/FlagMemorialQueryServiceTest.java` | 신규 |
