# PLAN: Flag 목록 조회에 참여 인원수(participantCount) 추가

## 목표
Flag 목록 조회 API(`/api/v1/flags/me`, `/api/v1/flags/friends`)의 응답에
현재 참여 인원수(`participantCount`)를 포함시킨다.

## 현황 분석

| 항목 | 현재 상태 |
|------|---------|
| `FlagResult` | `capacity`만 있음, `participantCount` 없음 |
| `FlagDetailResult` | `participantCount` 있음 (상세 조회는 이미 지원) |
| `FlagRepository` | 단건 `countParticipants(Long flagId)` 존재 |
| 목록 조회 서비스 | count 쿼리를 전혀 호출하지 않음 |

## 설계 결정

### N+1 방지를 위한 Bulk Count 쿼리
목록 조회 시 플래그가 N개이면 단건 `countByFlagId`를 N번 호출하게 되므로,
`flagIds IN (...)` 기반의 단일 쿼리로 일괄 조회 후 `Map<Long, Integer>`로 변환한다.

### participantCount 정의
`FlagParticipant` 테이블 기준 카운팅 (host 제외 순수 참여자 수)

---

## 변경 파일 (5개)

### 1. `FlagParticipantJpaRepository.java`
Bulk count JPQL 쿼리 및 projection 인터페이스 추가:
```java
interface FlagParticipantCountProjection {
    Long getFlagId();
    Long getCount();
}

@Query("SELECT fp.flagId as flagId, COUNT(fp) as count FROM FlagParticipant fp WHERE fp.flagId IN :flagIds GROUP BY fp.flagId")
List<FlagParticipantCountProjection> countByFlagIdIn(@Param("flagIds") Collection<Long> flagIds);
```

### 2. `FlagRepository.java` (domain port)
메서드 추가:
```java
Map<Long, Integer> countParticipantsByFlagIds(Collection<Long> flagIds);
```

### 3. `FlagRepositoryAdapter.java`
구현 추가:
```java
@Override
public Map<Long, Integer> countParticipantsByFlagIds(Collection<Long> flagIds) {
    if (flagIds == null || flagIds.isEmpty()) return Map.of();
    return participantJpaRepository.countByFlagIdIn(flagIds).stream()
            .collect(Collectors.toMap(
                FlagParticipantJpaRepository.FlagParticipantCountProjection::getFlagId,
                p -> p.getCount().intValue()
            ));
}
```

### 4. `FlagResult.java`
`participantCount` 필드 추가 및 `of()` 시그니처 변경:
```java
public record FlagResult(
    Long id,
    String title,
    String description,
    int capacity,
    int participantCount,   // 추가
    String status,
    FlagScheduleResult schedule,
    FlagHostResult host
) {
    public static FlagResult of(Flag flag, FlagUserInfo hostInfo, int participantCount) { ... }
}
```

### 5. `FlagQueryService.java`
세 개 목록 조회 메서드에서 bulk count 활용:
- `getFriendFlags()` — `recruitingFlags`의 ID 목록으로 bulk count 후 매핑
- `getMyHostingFlags()` — `managedFlags`의 ID 목록으로 bulk count 후 매핑
- `getParticipatingFlags()` — `flags`의 ID 목록으로 bulk count 후 매핑

---

## 영향 범위
- `FlagDetailResult.of()`는 `participants.size()`를 직접 사용하므로 영향 없음
- 기존 `countParticipants(Long flagId)` 단건 메서드는 `FlagManagementService`에서 사용 중 → 유지

## 브랜치
`ai/feat-flag-participant-count` (from main)