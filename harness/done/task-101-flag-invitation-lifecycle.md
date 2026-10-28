# Task-101: FlagInvitation 수명 관리

> **Domain Change:** [ ] — `FlagInvitation` 엔티티의 필드를 건드리지 않는다.
> 조회 필터와 정리 스윕만 추가한다.

> **FE 협조 필요:** [ ] — 응답 스키마는 그대로다. 다만 목록에서 항목이 빠지므로
> 배포 후 초대 개수가 줄어드는 것은 정상이다.

> **선행 조건:** 두 Phase를 같은 브랜치에서 함께 배포하되 커밋은 분리한다.
> Phase 1 커밋이 먼저다 — 두 Phase가 서로를 대체하지 않는 이유는 아래 참조.

## Background

### 초대는 만료 개념을 소유하지 않는다 — 의도된 설계다

`a105a79`에서 `expiresAt` 스냅샷을 제거했다. 초대 시점 `deadline`의 복사본이라
호스트가 일정을 바꿔도 갱신되지 않았고, 권위 있는 판정은
`accept` → `participateByInvitation` → `Flag.participate()`의 `isRecruiting()`이
이미 수행하고 있었기 때문이다.

**이 task는 그 결정을 뒤집지 않는다.** 필터도 `Flag`에 묻고, 삭제 조건도 `Flag`의
`endDateTime`에서 끌어온다. `FlagInvitation`에 시간·상태 필드를 다시 만들지 않는다.

### 문제 1 — 수락할 수 없는 초대가 목록에 보인다

`FlagInvitationQueryService`의 필터는 하나뿐이다 (`:41`, `:57`).

```java
.filter(inv -> flagMap.containsKey(inv.getFlagId()) && userMap.containsKey(...))
```

`flagMap`은 `flagRepository.findAllByIdIn()`이고, `Flag`의
`@SQLRestriction("deleted_at IS NULL")` 때문에 **소프트 삭제된 것만** 빠진다.
마감·종료 여부는 보지 않는다.

| 시점 | 목록 노출 | 수락 시 |
|---|---|---|
| deadline 경과 ~ 모임 종료 (WAITING) | **노출됨** | `FlagDeadlinePassedException` (409) |
| 종료 후 ~ 24h | **노출됨** | 409 |
| 종료 +24h 이후, 일반 플래그 | 만료 스윕이 소프트 삭제 → 사라짐 | — |
| 종료 +24h 이후, `autoExpiryExempt` | **영구 노출** | **영구 409** |

DTO(`ReceivedFlagInvitationResult`, `SentFlagInvitationResult`)에 상태·마감 필드가
없으므로 **FE는 만료 여부를 판단할 수단이 아예 없다.** 만료된 초대가 살아 있는 초대와
픽셀 단위로 동일하게 렌더된다.

### 문제 2 — 정리되지 않는 초대 행이 있다

**퍼지는 이미 초대를 지우고 있다.** `FlagMaintenanceAdapter:68`의
`invitationJpaRepository.hardDeleteByFlagIdsIn(chunk)`가 그 일을 한다.
경로별로 따지면 누수는 한 곳뿐이다.

| 경로 | 초대 행 정리 | |
|---|---|---|
| 호스트가 직접 삭제 | `deletedAt` 설정 → 12h 뒤 퍼지 | ✅ |
| 종료 +24h, 일반 플래그 | 만료 스윕 → 퍼지 | ✅ |
| 종료 +24h, `autoExpiryExempt = true` | **소프트 삭제 자체가 안 됨** → 퍼지 대상이 된 적이 없음 | ❌ |

`expireAllExceedingThreshold`에 `AND f.autoExpiryExempt = false`가 있어
(`FlagJpaRepository:37`), 후기(memorial)가 달렸거나 앵코르 자식이 생긴 플래그는
`deleted_at`이 영원히 `null`이다. 퍼지는 `deleted_at < :bufferTime`으로 대상을 찾으므로
이 플래그의 초대 행은 무기한 남는다.

**따라서 이 Phase는 새 스케줄러도, 새 컬럼도, DDL도 필요 없다.**
`autoExpiryExempt` 하나 때문에 생긴 구멍을 메우는 일이다.

### 두 문제는 서로를 대체하지 않는다

- Phase 2가 완벽히 돌아도 **스윕 주기(6h) 사이 구간**과 **WAITING 구간**은 남는다.
  WAITING 플래그는 멀쩡히 살아 있으므로 행을 지워서는 안 된다(아래 2-1 참조).
  그 구간의 노출은 Phase 1만이 막는다.
- Phase 1이 있어도 행은 계속 쌓인다. 조회 때마다 fetch해서 버리므로
  `findAllByInviteeId` + `findAllByIdIn` + `findUserInfosByIds` 비용이 죽은 행 수만큼 늘어난다.

**Phase 1은 2줄 + 테스트고 사용자가 체감하는 쪽이다. Phase 2는 스케줄러 작업이다.**
성격이 다르므로 커밋을 분리해 각각 독립적으로 되돌릴 수 있게 둔다.

---

## Phase 1 — 조회 필터

### 1-1. `isRecruiting()`이 맞는 술어다

`Flag.participate()`의 가드가 `if (!this.isRecruiting()) throw new FlagDeadlinePassedException()`이다.
이 술어로 거르면 **"목록에 보이는 것 = 누르면 되는 것"이 정확히 일치한다.**

`!isEnded()`로 거르면 안 된다. WAITING(마감 경과, 모임 미시작)이 살아남아
보이는데 409 나는 항목이 그대로 남는다.

### 1-2. 받은 목록·보낸 목록 둘 다 적용한다

보낸 목록도 거른다. **DTO에 상태 필드가 없어 만료된 것과 살아 있는 것이 구분되지 않기 때문이다.**
호스트가 "아직 답 기다리는 중"으로 읽을 수밖에 없는데 실제로는 아무도 수락할 수 없다.
상태를 못 보여줄 거면 숨기는 쪽이 덜 틀리다.

숨겨도 깨지는 흐름은 없다. 중복 초대 방어는 FE 목록이 아니라
`existsByFlagIdAndInviteeId`가 DB에서 하고, 모집 중이 아닌 플래그로의 초대는
`FlagInvitationManager.invite()`가 통째로 막는다.

### 1-3. 인메모리로 거른다 — 쿼리로 내리지 않는다

`flagMap`은 이미 가져오고 있으므로 술어 한 줄이면 된다.

```java
.filter(inv -> {
    Flag flag = flagMap.get(inv.getFlagId());
    return flag != null && flag.isRecruiting() && userMap.containsKey(...);
})
```

`findByInviteeIdAndFlagDeadlineAfter` 같은 쿼리로 내리면
**RECRUITING 판정이 `FlagSchedule.calculateStatus`와 리포지토리 두 군데로 갈라진다.**
"DB 쿼리 이름에는 필드 비교 술어만, 상태 어휘는 도메인 소유" 원칙에도 어긋난다.

두 메서드가 같은 술어를 쓰므로 private 헬퍼로 뽑아도 좋다.

### Phase 1 검증

- **기존 테스트는 안 깨진다.** `FlagInvitationQueryServiceTest.buildFlag()`가
  deadline을 `NOW.plusHours(1)`로 잡아 RECRUITING이다. **통과가 곧 검증이 아니다.**
- 추가할 케이스 — 받은/보낸 각각:
  - deadline 경과, 모임 미시작(WAITING) → 제외
  - 모임 진행 중(IN_ACTIVITY) → 제외
  - 종료됨(ENDED) → 제외
  - RECRUITING과 만료된 것이 섞여 있을 때 → RECRUITING만 반환
- `FlagInvitationControllerTest`에 만료 초대가 목록에 안 나오는 통합 케이스 하나

---

## Phase 2 — 종료된 플래그의 초대 정리

### 2-1. 트리거는 ENDED다. deadline이 아니다.

**이 절이 Phase 2에서 가장 중요하다.**

`reschedule()`의 가드는 `isBeforeActivity()`(RECRUITING + WAITING)다. 즉 마감이 지나
WAITING이 된 플래그도 호스트가 일정을 뒤로 밀면 deadline이 다시 미래가 되어
**RECRUITING으로 복귀한다.** `closeRecruitment()`로 수동 마감한 경우도 마찬가지다.

deadline 기준으로 초대를 지우면 되살아난 플래그의 초대받은 사람들이 영문도 모르고
초대를 잃는다. **소프트 삭제에 복구 경로가 없듯 이것도 되돌릴 수 없다.**

반면 ENDED는 일방통행이다 — `reschedule()`이 `validateNotEnded()`로 막고
`isBeforeActivity()`로 한 번 더 막는다. ENDED 기준 삭제는 안전하다.

### 2-2. `FlagExpiryService`에 단계를 추가한다

새 스케줄러를 만들지 않는다. `expireEndedFlags()`가 이미 6시간마다 돌고
이미 `threshold`(`now - 24h`)를 계산한다.

```java
@Transactional
public void expireEndedFlags() {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime threshold = now.minusHours(Flag.EXPIRATION_THRESHOLD_HOURS);

    int expired = flagRepository.expireAllExceedingThreshold(threshold, now);
    int purgedInvitations = /* 아래 2-3 */;
    ...
}
```

**같은 `threshold`를 재사용한다.** 초대는 deadline 시점부터 이미 무용하지만,
개념을 하나 더 만들지 않는 편이 낫고 24h 유예는 보수적인 쪽으로 안전하다.

`expireAllExceedingThreshold`와 달리 **`autoExpiryExempt`로 거르지 않는다.**
exempt를 무시하는 것이 이 Phase의 존재 이유다. 후기·앵코르는 플래그를 보존할
이유가 되지만 미응답 초대를 보존할 이유는 아니다.

### 2-3. 쿼리 — `@SQLRestriction` 함정 주의

```java
@Modifying(clearAutomatically = true, flushAutomatically = true)
@Query("DELETE FROM FlagInvitation fi WHERE fi.flagId IN " +
       "(SELECT f.id FROM Flag f WHERE f.schedule.endDateTime < :threshold)")
int hardDeleteByFlagEndDateTimeBefore(@Param("threshold") LocalDateTime threshold);
```

**`Flag`의 `@SQLRestriction("deleted_at IS NULL")`이 서브쿼리에도 적용된다.**
즉 소프트 삭제된 플래그는 서브쿼리에서 빠진다. 그쪽은 퍼지가 이미 처리하므로
결과적으로 옳지만, **의도적인 것임을 주석으로 남긴다.** `FlagJpaRepository:45`에
같은 함정을 우회한 선례가 있다.

메서드 이름은 필드 비교 술어만 쓴다. `deleteExpiredInvitations` 같은 상태 어휘를
리포지토리에 넣지 않는다.

### 2-4. 배치는 지금 넣지 않는다

최초 1회 실행 시 그동안 쌓인 exempt 플래그의 초대가 한 번에 지워진다.
현재 규모에서는 문제되지 않으므로 배치를 넣지 않되, **최초 실행 로그의 건수를 확인한다.**
예상보다 크면 `LIMIT` 배치로 나눈다(JPQL은 `DELETE ... LIMIT`를 지원하지 않으므로
`FlagPurgeService`처럼 ID를 먼저 뽑는 방식이나 네이티브 쿼리가 필요하다).

### 2-5. 확인된 안전성

- **앵코르 자동 초대는 안 깨진다.** `FlagEncoreInvitationListener`는 부모의
  *참여자* 목록(`findAllParticipantIds`)을 읽고, 초대는 앵코르 플래그 자기 것만
  조회한다(`:40`). 부모의 초대 행을 지워도 무관하다.
- 알림 metadata의 `invitationId`는 dangling이 될 수 있으나, 그 알림은 이미
  만료된 초대를 가리키고 있었다. 탭하면 `FlagInvitationNotFoundException`(404)이
  409 대신 나갈 뿐이다.

### 2-6. 인덱스는 없다 — 지금은 괜찮다

**task-96은 아직 착수 전이다.** Flyway 의존성도, `db/migration/` 디렉터리도 없고
`ddl-auto`는 여전히 `update`다. 따라서 `idx_flags_end_date_time`이 존재하지 않고
2-3의 서브쿼리는 `flags` 풀스캔이 된다.

**새로 생기는 문제가 아니다.** 바로 옆의 `expireAllExceedingThreshold`가 이미 같은
조건으로 풀스캔하고 있고, 둘 다 6시간 주기 백그라운드라 사용자 요청 경로 밖이다.
task-96 Phase 2가 인덱스를 넣으면 두 쿼리가 함께 혜택을 받는다.
**이 task에서 인덱스를 만들지 않는다.**

### Phase 2 검증

- 종료 +24h 경과 + `autoExpiryExempt = true` 플래그의 초대가 삭제되는가 — **본체 케이스**
- 종료 +24h 미경과 플래그의 초대는 남는가
- WAITING 플래그(마감 경과, 미시작)의 초대는 **남는가** — 2-1의 회귀 방지
- RECRUITING 플래그의 초대는 남는가
- 소프트 삭제된 플래그의 초대는 이 쿼리로 안 지워지고 퍼지가 지우는가 (2-3 확인)
- 최초 실행 로그의 삭제 건수 확인 (2-4)

---

## 커밋 분할

Phase 1과 Phase 2는 같은 브랜치에서 함께 배포한다. **커밋만 분리한다.**

```
fix(flag): 모집 중이 아닌 플래그의 초대를 목록에서 제외한다
feat(flag): 종료된 플래그의 초대 행을 만료 스윕에서 정리한다
```

각 커밋은 자체 테스트를 포함한다. 테스트를 뒤로 몰지 않는다.

## Out of Scope

- **`FlagInvitation`에 `status`·`expiresAt` 재도입.** `a105a79`에서 제거한 결정을
  뒤집지 않는다. 만료 판정의 권위는 `Flag` 하나다.
- **DTO에 `flagStatus`·`deadline` 추가.** "마감됨" 회색 표시를 하려면 필요하지만
  FE 동반 작업이다. 필요해지면 별도 task.
- **모든 DDL·인덱스.** task-96(**미착수**)이 Flyway를 들여온 뒤 처리할 일이며,
  **이 task는 스키마를 전혀 건드리지 않는다.** 아래 둘은 task-96이 처리할 항목이지
  이미 처리된 항목이 아니다.
  - `flag_invitations.expires_at` / `status` 컬럼 제거 — 운영 DB에 아직 남아 있다.
    엔티티에 대응 필드가 없어 무해하고 Phase 2의 DELETE도 건드리지 않는다.
  - `idx_flags_end_date_time`, `idx_flag_invitations_*` — 2-6 참조
- 초대 만료 시 초대자에게 알림 — 요구된 적 없다
- 만료된 초대의 히스토리 보관 — 지금은 지우는 게 맞다. 보관이 필요해지면
  삭제가 아니라 별도 테이블 설계 문제다
