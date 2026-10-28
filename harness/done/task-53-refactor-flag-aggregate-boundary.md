# Task-53: Flag 도메인 Aggregate 경계 재정립

## 배경

현재 Flag 도메인은 `FlagParticipant`가 독립 Repository를 가지면서 `Flag`와 강하게 결합된 애매한 구조다.
`FlagInvitation`은 독립 Aggregate임에도 `flag.domain.flag` 패키지에 묻혀 있어 경계가 시각적으로 불명확하다.

## 목표

1. **FlagParticipant → Flag 내부 엔티티화**
   - `FlagParticipantRepository` 인터페이스 제거
   - 해당 메서드를 `FlagRepository`로 흡수
   - `FlagRepositoryAdapter`가 `FlagJpaRepository` + `FlagParticipantJpaRepository` 모두 참조
   - `FlagParticipationManager`, `FlagInvitationManager` 등 Participant 접근 시 `FlagRepository` 단일 경유

2. **FlagInvitation → `flag.domain.invitation` 패키지 재분리**
   - `FlagInvitation`, `FlagInvitationStatus`, `FlagInvitationManager`
   - 관련 event, exception, repository도 함께 이동
   - 독립 Aggregate임을 패키지 구조로 명시

## 변경 대상 파일 (예상)

### 제거
- `flag/domain/flag/repository/FlagParticipantRepository.java`
- `flag/adapter/out/persistence/FlagParticipantRepositoryAdapter.java`

### 수정
- `flag/domain/flag/repository/FlagRepository.java` — Participant 메서드 흡수
- `flag/adapter/out/persistence/FlagRepositoryAdapter.java` — FlagParticipantJpaRepository 추가 참조
- `FlagParticipationManager.java` — participantRepository 의존 제거
- `FlagInvitationManager.java` — participantRepository 의존 제거
- `FlagManagementService.java`, `FlagInvitationService.java` 등 — 참조 교체

### 이동 (flag.domain.flag → flag.domain.invitation)
- `FlagInvitation.java`
- `FlagInvitationStatus.java`
- `FlagInvitationManager.java`
- `event/FlagInvitationSentEvent.java`
- `exception/FlagInvitation*.java`
- `repository/FlagInvitationRepository.java`

## 주의사항

- `FlagParticipantJpaRepository`는 인프라 계층에 유지 (JPA 특성상 Participant 저장은 Adapter에서 직접 처리)
- `FlagMaintenanceAdapter`의 `hardDeleteByFlagIdsIn`은 변경 없음 (인프라 레이어 독립)
- 도메인 내 Participant 접근은 모두 `FlagRepository`를 통하도록 일관성 유지

## 브랜치

`ai/refactor-flag-aggregate-boundary`
