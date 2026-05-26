# PLAN: Task-46 Flag 초대 기능

## 작업 목표

호스트가 참여자에게 초대 권한을 부여하면 해당 참여자가 자신의 친구를 초대할 수 있다. 초대받은 사람은 수락/거절할 수 있으며, 수락 시 호스트-친구 검증 없이 참여자가 된다. Flag 모집 중(`RECRUITING`) 상태에서만 초대 가능.

---

## 현황 분석

- `FlagParticipant`에 `canInvite` 필드 없음
- `FlagInvitation` 도메인 없음
- 현재 참여 경로(`FlagParticipationPolicy.participate`)는 항상 친구 검증 포함
- `Flag.participate(userId, count)`는 패키지-프라이빗 → `FlagInvitationPolicy`도 같은 패키지에 두면 재사용 가능
- `FlagMaintenanceAdapter.purgeFlagsAndRelatedData`는 flag 물리 삭제 시 연관 데이터 삭제 — 초대 테이블도 추가 필요

---

## 변경 파일 목록

### 신규 생성

| 파일 | 내용 |
|------|------|
| `flag/domain/flag/FlagInvitationStatus.java` | `PENDING / ACCEPTED / REJECTED` enum |
| `flag/domain/flag/FlagInvitation.java` | JPA 엔티티. `create()`, `accept()`, `reject()`, `isExpired()` |
| `flag/domain/flag/repository/FlagInvitationRepository.java` | 도메인 포트 |
| `flag/domain/flag/FlagInvitationPolicy.java` | @Component. `updateInvitePermission`, `invite`, `accept`, `reject` |
| `flag/domain/flag/exception/FlagInvitationNotFoundException.java` | 404 |
| `flag/domain/flag/exception/FlagInvitationDuplicateException.java` | 409 |
| `flag/domain/flag/exception/FlagInvitationExpiredException.java` | 400 |
| `flag/domain/flag/exception/FlagInvitationAccessException.java` | 403 |
| `flag/application/port/in/FlagInvitationUseCase.java` | 인터페이스 |
| `flag/application/service/flag/FlagInvitationService.java` | @Service, 정책 위임 + 이벤트 발행 |
| `flag/application/port/in/command/FlagInviteCommand.java` | flagId, inviterId, inviteeId |
| `flag/application/port/in/command/FlagInvitePermissionCommand.java` | flagId, requesterId, participantUserId, canInvite |
| `flag/adapter/in/web/FlagInvitationController.java` | `POST /api/v1/flag-invitations/{id}/accept`, `/reject` |
| `flag/adapter/in/web/dto/FlagInviteRequest.java` | `{ inviteeId }` |
| `flag/adapter/in/web/dto/FlagInvitePermissionRequest.java` | `{ canInvite }` |
| `flag/adapter/out/persistence/FlagInvitationRepositoryAdapter.java` | 어댑터 |
| `flag/adapter/out/persistence/jpa/FlagInvitationJpaRepository.java` | JPA |
| `flag/application/eventListener/FlagInvitationEventListener.java` | AFTER_COMMIT + @Async, 초대 알림 발행 |
| `flag/domain/flag/event/FlagInvitationSentEvent.java` | record(flagId, invitationId, inviteeId, flagTitle) |

### 수정

| 파일 | 변경 내용 |
|------|-----------|
| `FlagParticipant.java` | `canInvite` 필드 추가, `grantInvitePermission()` / `revokeInvitePermission()` |
| `Flag.java` | `grantInvitePermission(requesterId, participant)`, `revokeInvitePermission(requesterId, participant)` — 호스트 검증 후 위임 |
| `FlagController.java` | `PATCH /{flagId}/participants/{participantId}/invite-permission`, `POST /{flagId}/invitations` 엔드포인트 추가 |
| `FlagMaintenanceAdapter.java` | `purgeFlagsAndRelatedData`에 `FlagInvitationJpaRepository.hardDeleteByFlagIdsIn` 추가 |
| `NotificationType.java` | `FLAG_INVITATION` 추가 |

---

## 구현 방향

### 패키지 배치
`FlagInvitationPolicy`를 `flag.domain.flag` 패키지에 둔다. `Flag.participate()`가 패키지-프라이빗이므로 별도 공개 경로 추가 없이 재사용 가능.

### invite-permission URL의 participantId
`PATCH /flags/{flagId}/participants/{participantId}/invite-permission`에서 `participantId`는 **참여자의 userId** (auto-id가 아님). `FlagParticipantRepository.findByFlagIdAndParticipantId`로 조회.

### accept 흐름 (race condition 처리)
1. `FlagInvitation` 로드 → PENDING/미만료/inviteeId 검증
2. `isParticipating` 중복 참여 검증
3. `flagRepository.findByIdExclusive` (pessimistic lock)
4. `flag.participate(inviteeId, count)` — 상태·정원 검증 + `FlagParticipant` 생성
5. `invitation.accept()` 상태 전환

### expiresAt
`FlagInvitation.expiresAt = flag.schedule.deadline` — flag 모집 마감 = 초대 만료.

### 알림
`FlagInvitationService`에서 초대 저장 후 `FlagInvitationSentEvent` 발행 → `FlagInvitationEventListener`가 AFTER_COMMIT으로 `NotificationEvent` 발행 (invitee에게 `FLAG_INVITATION` 푸시).

---

## 예상 사이드 이펙트

- `flag_participants` 테이블에 `can_invite BOOLEAN DEFAULT FALSE` 컬럼 추가 (Hibernate DDL auto)
- `flag_invitations` 신규 테이블 생성
- `FlagMaintenanceAdapter` 미수정 시 flag 물리 삭제 시 초대 데이터 고아 레코드 발생

---

## 테스트 전략

**단위 테스트**

`FlagInvitationPolicyTest` (MockitoExtension):
- `invite_Success`
- `invite_InviterHasNoPermission_Throws`
- `invite_DuplicatePending_Throws`
- `invite_InviteeIsHost_Throws`
- `invite_InviteeAlreadyParticipating_Throws`
- `accept_Success`
- `accept_AlreadyParticipating_Throws`
- `accept_Expired_Throws`
- `accept_NotInvitee_Throws`
- `reject_Success`
- `updateInvitePermission_Grant_Success`
- `updateInvitePermission_NotHost_Throws`

`FlagInvitationServiceTest` (MockitoExtension):
- 정책 위임 및 이벤트 발행 검증
