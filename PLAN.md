# PLAN: task-75 — Flag 초대 취소 및 수락/거절 후 초대 삭제

## 작업 목표

1. 초대 취소 API 추가 (`DELETE /api/v1/flag-invitations/{invitationId}`) — inviter만 PENDING 초대를 취소 가능
2. 수락·거절 처리 후 `flag_invitations` 행을 DB에서 삭제 (상태 업데이트 → 삭제로 변경)

---

## 현황 분석

| 항목 | 현재 상태 |
|------|----------|
| `FlagInvitationUseCase` | `invite`, `accept`, `reject`, `updateInvitePermission` — cancel 없음 |
| `FlagInvitationController` | `POST /{id}/accept`, `POST /{id}/reject` — DELETE 엔드포인트 없음 |
| `FlagInvitationManager.accept()` | `invitation.accept()` → status=ACCEPTED 후 행 유지 |
| `FlagInvitationManager.reject()` | `invitation.reject()` → status=REJECTED 후 행 유지 |
| `FlagInvitationService.accept/reject()` | dirty check로 status만 반영, `deleteById` 없음 |
| `FlagInvitationRepository` (port) | `deleteById` 없음 |
| `FlagInvitationJpaRepository` | `JpaRepository` 상속으로 `deleteById` 기본 제공 |

---

## 변경 파일 목록

### 1. `FlagInvitation` (domain)
- `accept()`, `reject()` — status 변경 코드 제거, **검증만** 수행하도록 축소
  - `accept()`: invitee 체크 + pending 체크 + expired 체크
  - `reject()`: invitee 체크 + pending 체크
- `cancel()` 메서드 추가 — inviter 체크 + pending 체크

### 2. `FlagInvitationRepository` (port/out)
- `void deleteById(Long id)` 추가

### 3. `FlagInvitationRepositoryAdapter`
- `deleteById` 구현 → `jpaRepository.deleteById(id)` 위임

### 4. `FlagInvitationManager`
- `accept()`: `invitation.accept()` 검증 후 참여자 생성 반환, status 변경 없음
- `reject()`: `invitation.reject()` 검증, status 변경 없음
- `cancel(Long invitationId, Long requesterId)` 추가 — 조회 후 `invitation.cancel()` 검증

### 5. `FlagInvitationUseCase` (port/in)
- `void cancel(Long invitationId, Long requesterId)` 추가

### 6. `FlagInvitationService`
- `accept()`: 참여자 저장 후 `invitationRepository.deleteById(invitationId)` 추가
- `reject()`: 검증 후 `invitationRepository.deleteById(invitationId)` 추가
- `cancel()` 구현: `invitationManager.cancel()` → `invitationRepository.deleteById(invitationId)`

### 7. `FlagInvitationController`
- `DELETE /{invitationId}` 엔드포인트 추가

---

## 구현 방향

- `FlagInvitation.accept/reject`에서 status 변경만 제거 — 행 자체를 삭제하므로 불필요
- `FlagInvitationStatus` enum(ACCEPTED, REJECTED)은 제거하지 않음 — 스키마 변경 리스크 회피
- 삭제는 서비스 레이어에서 담당, 도메인 객체는 검증만 책임

---

## 예상 사이드 이펙트

- `accept` 후 동일 초대 ID 재호출 → `findById` 결과 없어 `FlagInvitationNotFoundException` (정상)
- `reject` 후 재초대 허용 — `existsPendingByFlagIdAndInviteeId` 체크를 통과하므로 의도된 동작
- 기존 테스트에서 accept/reject 후 status 검증 assertions 수정 필요

---

## 테스트 전략

- `FlagInvitationServiceTest`: accept/reject 후 `invitationRepository.deleteById` 호출 verify 추가
- `FlagInvitationManagerTest`: `cancel()` 케이스 추가 (non-inviter 취소, 이미 처리된 초대 취소)
- 기존 status assertion → `deleteById` 호출 검증으로 교체

---

## 변경 파일 요약

| 파일 | 유형 |
|------|------|
| `flag/domain/invitation/FlagInvitation.java` | 수정 |
| `flag/domain/invitation/repository/FlagInvitationRepository.java` | 수정 |
| `flag/adapter/out/persistence/FlagInvitationRepositoryAdapter.java` | 수정 |
| `flag/domain/invitation/FlagInvitationManager.java` | 수정 |
| `flag/application/port/in/FlagInvitationUseCase.java` | 수정 |
| `flag/application/service/flag/FlagInvitationService.java` | 수정 |
| `flag/adapter/in/web/FlagInvitationController.java` | 수정 |
| `flag/application/service/flag/FlagInvitationServiceTest.java` | 수정 |
| `flag/domain/invitation/FlagInvitationManagerTest.java` | 수정 |
