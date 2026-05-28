# PLAN: Task-53 Flag 도메인 Aggregate 경계 재정립

## 작업 목표

1. `FlagParticipant`를 Flag Aggregate의 내부 엔티티로 전환 — `FlagParticipantRepository` 제거, 기능을 `FlagRepository`로 흡수
2. `FlagInvitation` 관련 클래스를 `flag.domain.invitation` 패키지로 분리 — 독립 Aggregate임을 구조로 표현

---

## 현황 분석

### FlagParticipantRepository 의존 클래스 (12곳)

| 클래스 | 사용 메서드 |
|---|---|
| `FlagParticipationManager` | `isParticipating`, `countByFlagId`, `findByFlagIdAndParticipantId` |
| `FlagInvitationManager` | `findByFlagIdAndParticipantId`, `isParticipating` |
| `FlagParticipationService` | `save`, `delete` |
| `FlagInvitationService` | `save` |
| `FlagManagementService` | `countByFlagId` |
| `FlagQueryService` | `findAllByFlagId`, `findFlagIdByParticipantId` |
| `FlagMemorialFactory` | `findAllParticipantIdsByFlagId` |
| `FlagCommentCommandService` | `findAllParticipantIdsByFlagId` |
| `FlagCommentQueryService` | `findAllParticipantIdsByFlagId` |
| `FlagDeletionEventListener` | `findAllParticipantIdsByFlagId`, `deleteAllByFlagId` |
| `FlagEncoreEventListener` | `findAllParticipantIdsByFlagId` |
| `FlagMeetingChangedEventListener` | `findAllParticipantIdsByFlagId` |

### FlagInvitation 이동 대상 (flag.domain.flag → flag.domain.invitation)

- `FlagInvitation.java`
- `FlagInvitationStatus.java`
- `FlagInvitationManager.java`
- `event/FlagInvitationSentEvent.java`
- `exception/FlagInvitation{Access,Duplicate,Expired,NotFound}Exception.java`
- `repository/FlagInvitationRepository.java`

---

## 변경 파일 목록

### 수정
| 파일 | 변경 내용 |
|---|---|
| `flag/domain/flag/repository/FlagRepository.java` | `FlagParticipantRepository`의 모든 메서드 흡수 |
| `flag/adapter/out/persistence/FlagRepositoryAdapter.java` | `FlagParticipantJpaRepository` 주입 추가, Participant 메서드 구현 |
| `FlagParticipationManager.java` | `flagParticipantRepository` → `flagRepository` |
| `FlagInvitationManager.java` | `participantRepository` → `flagRepository` |
| `FlagParticipationService.java` | `participantRepository` → `flagRepository` |
| `FlagInvitationService.java` | `participantRepository` → `flagRepository` |
| `FlagManagementService.java` | `participantRepository` → `flagRepository` |
| `FlagQueryService.java` | `participantRepository` → `flagRepository` |
| `FlagMemorialFactory.java` | `flagParticipantRepository` → `flagRepository` |
| `FlagCommentCommandService.java` | `participantRepository` → `flagRepository` |
| `FlagCommentQueryService.java` | `participantRepository` → `flagRepository` |
| `FlagDeletionEventListener.java` | `participantRepository` → `flagRepository` |
| `FlagEncoreEventListener.java` | `participantRepository` → `flagRepository` |
| `FlagMeetingChangedEventListener.java` | `participantRepository` → `flagRepository` |
| `FlagInvitationService.java` | import 경로 수정 (invitation 패키지) |
| `FlagInvitationEventListener.java` | import 경로 수정 |
| `FlagInvitationController.java` | import 경로 수정 |
| `FlagInvitationUseCase.java` | import 경로 수정 |

### 제거
| 파일 | 이유 |
|---|---|
| `flag/domain/flag/repository/FlagParticipantRepository.java` | FlagRepository로 흡수 |
| `flag/adapter/out/persistence/FlagParticipantRepositoryAdapter.java` | FlagRepositoryAdapter로 통합 |

### 이동 (패키지 변경)
`flag.domain.flag.*` → `flag.domain.invitation.*`

---

## 구현 방향

### FlagRepository 확장

```java
// 기존 Flag 메서드 유지 +
FlagParticipant saveParticipant(FlagParticipant participant);
void deleteParticipant(FlagParticipant participant);
Optional<FlagParticipant> findParticipant(Long flagId, Long participantId);
int countParticipants(Long flagId);
boolean isParticipating(Long flagId, Long participantId);
List<Long> findAllParticipantIds(Long flagId);
void deleteAllParticipants(Long flagId);
List<Long> findFlagIdsByParticipantId(Long participantId);
List<FlagParticipant> findAllParticipants(Long flagId);
```

메서드명에 `Participant` 명사를 포함시켜 Flag 자체 메서드와 시각적으로 구분.

### FlagMaintenanceAdapter

`hardDeleteByFlagIdsIn`은 인프라 레이어 내부에서 직접 `FlagParticipantJpaRepository`를 참조하므로 변경 없음.

### 트랜잭션 일관성

`FlagParticipationService.participate()` / `unparticipate()`는 서비스 레이어에서 `flagRepository.saveParticipant()`를 호출. 도메인 메서드(`Flag.participate()`)가 FlagParticipant 인스턴스를 생성하고, 서비스가 이를 저장하는 현재 흐름 유지.

---

## 예상 사이드 이펙트

- `FlagInvitationManager`가 `flag.domain.invitation` 패키지로 이동하면서 `FlagInvitationService`의 import 변경
- `FlagInvitationManagerTest`도 패키지 이동 필요
- 컴파일 에러를 따라가며 순차 수정

## 브랜치

`ai/refactor-flag-aggregate-boundary` (from main)
