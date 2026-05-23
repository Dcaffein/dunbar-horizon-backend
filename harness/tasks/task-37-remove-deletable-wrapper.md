# Task-37: Deletable<T> 래퍼 패턴 제거

## 배경

`Deletable<T>`는 "권한 검증을 통과한 삭제 티켓"을 타입으로 표현하려는 의도로 도입되었다.
그러나 실질적인 보호 효과가 제한적이다.

- 도메인 메서드가 이미 예외를 던지기 때문에 래퍼 없이도 동일한 보호 수준
- 어댑터에서 결국 `.getEntity()`로 언래핑 후 `jpaRepository.delete(entity)` 호출
- 엔티티마다 `DeletableXxx` 구체 클래스가 필요해 보일러플레이트 증가
- `FlagComment.validateDeletionAuthority()` 패턴(task-34)과 불일치

## 목표

`Deletable<T>` 계층 전체를 제거하고 `FlagComment` 패턴과 일관되게 통일한다.
도메인 객체 내부에서 검증 후 예외를 던지고, 서비스/Policy가 엔티티를 직접 repository에 넘긴다.

## 변경 내용

### 도메인 객체
| 파일 | 변경 |
|------|------|
| `Flag.java` | `unparticipate()` DeletableParticipant 반환 → void 반환 |
| `FlagMemorial.java` | `verifyForDeletion()` → `validateDeletion()`, void 반환 |

### 포트
| 파일 | 변경 |
|------|------|
| `FlagParticipantRepository.java` | `delete(DeletableParticipant)` → `delete(FlagParticipant)` |
| `FlagMemorialRepository.java` | `delete(DeletableFlagMemorial)` → `delete(FlagMemorial)` |

### 어댑터
| 파일 | 변경 |
|------|------|
| `FlagParticipantRepositoryAdapter.java` | `delete(FlagParticipant)` → `jpaRepository.delete(participant)` |
| `FlagMemorialRepositoryAdapter.java` | `delete(FlagMemorial)` → `jpaRepository.delete(memorial)` |

### 서비스 / Policy
| 파일 | 변경 |
|------|------|
| `FlagParticipationPolicy.java` | `unparticipate()` → `FlagParticipant` 반환 |
| `FlagParticipationService.java` | `participantRepository.delete(participant)` 직접 호출 |
| `FlagMemorialCommandService.java` | `validateDeletion()` + `memorialRepository.delete(memorial)` |

### 삭제
- `global/common/Deletable.java`
- `flag/domain/flag/DeletableParticipant.java`
- `flag/domain/memorial/DeletableFlagMemorial.java`

## 체크리스트

- [ ] `Flag.unparticipate()` void 반환으로 변경
- [ ] `FlagMemorial.verifyForDeletion()` → `validateDeletion()`
- [ ] `FlagParticipantRepository` 포트 시그니처 변경
- [ ] `FlagMemorialRepository` 포트 시그니처 변경
- [ ] `FlagParticipantRepositoryAdapter` 구현 업데이트
- [ ] `FlagMemorialRepositoryAdapter` 구현 업데이트
- [ ] `FlagParticipationPolicy.unparticipate()` FlagParticipant 반환
- [ ] `FlagParticipationService.leaveFlag()` 직접 delete 호출
- [ ] `FlagMemorialCommandService.deleteMemorial()` 단순화
- [ ] `Deletable.java`, `DeletableParticipant.java`, `DeletableFlagMemorial.java` 삭제
- [ ] 테스트 코드 업데이트
- [ ] 빌드 확인

## 브랜치

`ai/refactor-remove-deletable-wrapper`

## Result

- 브랜치:
- 커밋:
- 변경 파일:
