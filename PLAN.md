# PLAN — Task 69: Flag 앵콜 시 부모 참여자 자동 초대

## 작업 목표

앵콜 Flag 생성 후 부모 Flag 참여자 전원에게 `FlagInvitation`을 자동 생성한다.
알림은 기존 `FLAG_ENCORE` 푸시를 제거하고, `FLAG_INVITATION` 알림으로 단일화한다.

## 현황 분석

### 앵콜 이벤트 처리 현황

```
FlagHostService.encoreFlag()
  → FlagEncoreFactory.encore()
    → Flag.createEncore()  — FlagEncoreEvent 등록
  → flagRepository.save(encoreFlag)  — 커밋

[BEFORE_COMMIT] FlagEncoreEventListener.handleEncoreCreated()  — 보존 정책 갱신 (유지)
[AFTER_COMMIT, ASYNC] FlagEncoreEventListener.handle()          — FLAG_ENCORE 알림 (제거)
```

`FlagEncoreEvent`: `parentFlagId`, `hostId`, `title` — encore flag ID 미포함
(이벤트 등록 시점에 JPA `@Id` 미확정이므로 담을 수 없음)

→ 리스너에서 `flagRepository.findByParentId(parentFlagId)`로 encore flag를 조회한다.

### 초대 단건 흐름 (기존)

```
FlagInvitationService.invite()
  → FlagInvitationManager.invite()  — 검증 + FlagInvitation 생성
  → invitationRepository.save()
  → eventPublisher.publishEvent(FlagInvitationSentEvent)
    → [AFTER_COMMIT, ASYNC] FlagInvitationEventListener.handle()  — FLAG_INVITATION 알림
```

앵콜 자동 초대는 `FlagInvitationSentEvent`를 동일하게 발행하므로 이 흐름을 그대로 탄다.

### 알림 중복 문제 및 결정

`FLAG_ENCORE` 알림과 `FLAG_INVITATION` 알림이 같은 사용자에게 동시에 발송되면 혼란이 생긴다.
두 알림의 CTA가 다르기 때문이다.

- `FLAG_ENCORE`: "앵콜이 생성됐어요" → 사용자가 직접 참여 신청
- `FLAG_INVITATION`: "초대받았어요, 수락/거절하세요" → 초대장 처리

초대장이 생긴 시점에서 `FLAG_ENCORE` 알림은 불필요하다.
**결정: `FlagEncoreEventListener.handle()`의 알림 발송 로직을 제거한다.**

부모 참여자 0명인 경우 초대장도 없고 알림도 없는데, 알릴 대상 자체가 없으므로 문제없다.

### 부족한 인프라

`FlagInvitationRepository`에 벌크 경로가 없다.
- `save(FlagInvitation)` 단건만 존재
- N명 초대 시 N번 왕복이 발생하므로 벌크 메서드 신설 필요

## 변경 파일 목록

| 파일 | 변경 내용 |
|------|---------|
| `flag/application/eventListener/FlagEncoreEventListener.java` | `handle()` 메서드 내 알림 발송 로직 제거 (메서드 자체 삭제) |
| `flag/domain/invitation/repository/FlagInvitationRepository.java` | `saveAll`, `findPendingInviteeIdsByFlagId` 추가 |
| `flag/adapter/out/persistence/jpa/FlagInvitationJpaRepository.java` | `findPendingInviteeIdsByFlagId` JPQL 쿼리 추가 |
| `flag/adapter/out/persistence/FlagInvitationRepositoryAdapter.java` | 위 두 메서드 위임 구현 |
| `flag/application/eventListener/FlagEncoreInvitationListener.java` | 신규 — 앵콜 이벤트 수신 후 벌크 초대장 생성 + 초대 이벤트 발행 |

## 구현 방향

### 1. `FlagEncoreEventListener` — `handle()` 제거

`handleEncoreCreated()`(BEFORE_COMMIT, 보존 정책 갱신)는 유지한다.
`handle()`(AFTER_COMMIT, 알림 발송)은 삭제한다.
`flagRepository`, `eventPublisher` 의존성도 함께 제거한다.

### 2. `FlagInvitationRepository` 포트 확장

```java
List<FlagInvitation> saveAll(List<FlagInvitation> invitations);
Set<Long> findPendingInviteeIdsByFlagId(Long flagId);
```

### 3. `FlagInvitationJpaRepository` — JPQL 추가

```java
@Query("SELECT fi.inviteeId FROM FlagInvitation fi WHERE fi.flagId = :flagId AND fi.status = 'PENDING'")
Set<Long> findPendingInviteeIdsByFlagId(@Param("flagId") Long flagId);
```

`saveAll`은 `JpaRepository` 상속으로 이미 존재 — 선언 불필요.

### 4. `FlagEncoreInvitationListener` — 신규

```java
@Async
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void handle(FlagEncoreEvent event) {
    Flag encoreFlag = flagRepository.findByParentId(event.parentFlagId())
            .orElse(null);
    if (encoreFlag == null || !encoreFlag.isRecruiting()) return;

    List<Long> parentParticipantIds = flagRepository.findAllParticipantIds(event.parentFlagId());
    if (parentParticipantIds.isEmpty()) return;

    Set<Long> alreadyInvited      = invitationRepository.findPendingInviteeIdsByFlagId(encoreFlag.getId());
    Set<Long> alreadyParticipating = new HashSet<>(flagRepository.findAllParticipantIds(encoreFlag.getId()));

    List<FlagInvitation> invitations = parentParticipantIds.stream()
            .filter(id -> !id.equals(event.hostId()))
            .filter(id -> !alreadyInvited.contains(id))
            .filter(id -> !alreadyParticipating.contains(id))
            .map(id -> FlagInvitation.create(
                    encoreFlag.getId(), event.hostId(), id,
                    encoreFlag.getSchedule().getDeadline()))
            .toList();

    if (invitations.isEmpty()) return;

    List<FlagInvitation> saved = invitationRepository.saveAll(invitations);
    saved.forEach(inv -> eventPublisher.publishEvent(
            new FlagInvitationSentEvent(inv.getFlagId(), inv.getId(), inv.getInviteeId(), event.title())
    ));
}
```

## 예상 사이드 이펙트

- 앵콜 생성 API 응답 후 비동기 실행 — 응답 시점에 초대장 미존재는 의도된 결과
- `FlagEncoreEventListener`에서 `flagRepository`, `eventPublisher` 의존성이 제거됨에 따라 기존 테스트 수정 필요

## 테스트 전략

**`FlagEncoreEventListenerTest` 수정**
- `handle()` 관련 테스트 케이스 삭제
- `handleEncoreCreated()` 테스트는 유지

**`FlagEncoreInvitationListenerTest` 신규 (Mockito)**

| 케이스 | 검증 |
|--------|------|
| 정상 — 부모 참여자 3명 | 초대장 3개 생성, `saveAll` 1회, `FlagInvitationSentEvent` 3회 발행 |
| 호스트 본인이 부모 참여자에 포함 | 해당 ID 제외, 나머지만 초대 |
| 이미 pending 초대 존재 | 해당 ID 제외 |
| 이미 encore 참여 중 | 해당 ID 제외 |
| 부모 참여자 없음 | `saveAll` 미호출 |
| encore flag가 모집 중 아님 | `saveAll` 미호출 |
| encore flag 없음 | `saveAll` 미호출 |
