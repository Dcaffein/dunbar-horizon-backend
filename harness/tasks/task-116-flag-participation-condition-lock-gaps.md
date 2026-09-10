# Task-116: 참여 조건을 바꾸는 경로에 남은 잠금·격리 구멍 정리

## 목적

task-114·115에서 참여를 확정하는 두 경로만 다뤘다. 같은 규칙이 적용되어야 할 곳이 더 있고, 두 군데가 비어 있다. 그리고 참여 확정의 잠금·순서가 두 메서드에 중복돼 있어 새 경로가 생기면 다시 흩어진다.

## 배경

정원과 모집 마감을 판정하는 데 쓰는 값은 셋이다. 모집 중인지, 정원이 몇인지, 참여자가 몇 명인지. 앞의 둘은 `flags`의 필드이고 마지막은 `flag_participants`의 행 개수다.

이 값을 건드리는 명령마다 지켜야 할 것이 다르다.

| 명령 | 잠금 조회 | `READ_COMMITTED` | 현재 |
| --- | --- | --- | --- |
| 참여 (`participateInFlag`) | 필요 | 필요 | 둘 다 있음 |
| 초대 수락 (`accept`) | 필요 | 필요 | 둘 다 있음 |
| 정원 변경 (`modifyFlagCapacity`) | 필요 | 필요 | **격리 없음** |
| 모집 마감 (`closeRecruitment`) | 필요 | 불필요 | 잠금 있음 |
| 일정 변경 (`reschedule`) | 필요 | 불필요 | **잠금 없음** |
| 참여 취소 (`leaveFlag`) | 불필요 | 불필요 | 없음 (의도된 상태) |

`READ_COMMITTED`가 필요한 곳은 잠근 뒤에 참여자 수를 별도 조회로 세는 경로다. 참여자 취소는 수를 줄이는 방향이라 불변식을 깨지 않으므로 잠그지 않는다.

---

## 1. `reschedule`에 잠금 조회를 적용한다

`FlagModificationService.reschedule`은 `findById`로 읽고 `schedule`을 통째로 바꾼다. `schedule`에는 `deadline`이 들어 있고, 그 값은 참여 판정에 쓰인다. 같은 필드를 바꾸는 `closeRecruitment`는 잠금 조회를 쓰는데 이쪽만 쓰지 않는다.

`getFlagOrThrow`를 `findByIdForUpdate`로 바꾼다. 참여자 수를 읽지 않으므로 격리 수준 지정은 필요 없다.

**주의.** 이 변경으로 "마감 직후 일정을 다시 잡으면 마감이 풀리는" 동작이 막히지는 않을 수 있다. 잠금 조회는 판정에 쓰는 상태를 최신으로 만들 뿐이고, `Flag.reschedule`의 검증이 마감 이후 상태를 통과시키면 결과는 같다. **그 동작이 의도인지는 별도 판단이 필요하며 이 작업의 범위가 아니다.** 확인만 하고 결과를 기록한다.

## 2. `modifyFlagCapacity`에 `READ_COMMITTED`를 지정한다

이 메서드는 잠근 뒤 `countParticipants`로 참여자 수를 세고 그 값으로 정원 축소 가능 여부를 판정한다. 참여 확정 경로와 모양이 같다.

지금은 잠금 조회가 메서드의 첫 문장이라 읽기 뷰가 잠금 뒤에 잡혀 우연히 안전하다. 이 메서드 앞에 일반 조회가 하나라도 추가되면 참여자 수가 낡고, 호스트가 실제 참여자 수보다 작은 정원으로 줄일 수 있다. task-115에서 고친 것과 같은 종류다.

`FlagParticipationService.participateInFlag`와 같은 방식으로 메서드 단위로 지정하고, 근거를 주석으로 남긴다.

## 3. `FlagParticipationManager`의 참여 확정 부분을 한 메서드로 모은다

`participate`와 `participateByInvitation`은 친구 확인을 빼면 완전히 같다. 잠금 조회, 중복 검사, 참여자 수 조회, 참여자 생성 네 단계가 중복돼 있다.

확정 부분을 private 메서드로 추출해 잠금과 순서를 한 곳에 둔다. 새 참여 경로가 생겨도 그 메서드를 거치므로 "잠그고 나서 센다"를 다시 기억할 필요가 없어진다.

동작은 바뀌지 않는다. 기존 테스트가 그대로 통과해야 한다.

---

## 반드시 다룰 경계 조건

- **1번은 잠금 순서를 바꾼다.** `reschedule`이 잠금을 잡으면 참여 확정과 직렬화된다. 다른 명령과 잠금 획득 순서가 어긋나 교착이 생길 여지가 없는지 확인한다. 모두 `flags` 단일 행만 잠그므로 문제없을 것으로 보이나 확인한다.
- **2번의 지정 위치.** `FlagModificationService`는 클래스 레벨 `@Transactional`이 걸려 있다. 같은 클래스의 다른 메서드까지 바꾸지 않도록 메서드 단위로 지정한다. 그 메서드가 이미 열린 트랜잭션 안에서 호출되는 경로가 없는지 확인한다.
- **3번은 동작 변경이 아니다.** 추출 과정에서 검사 순서가 바뀌면 안 된다. 특히 친구 확인이 잠금보다 앞이라는 점은 task-27의 결정이므로 유지한다.

## 검증

현재 통합 테스트(`FlagParticipationLockConcurrencyIntegrationTest`)는 참여 확정 경로만 본다. 1번과 2번은 검증할 시나리오가 없다.

1. **일정 변경과 모집 마감의 경합.** 일정 변경이 Flag를 읽은 뒤 대기하는 동안 호스트가 모집을 마감하고 커밋한다. 잠금 적용 후 일정 변경이 최신 상태로 판정하는지 확인한다. 마감이 덮이는지 여부도 함께 기록한다.
2. **정원 변경이 앞선 참여의 커밋을 반영하는지.** 잠금 앞에 일반 조회가 있는 상태를 스파이로 재현한 뒤, 다른 트랜잭션이 참여를 커밋한다. `READ_COMMITTED` 없이는 낡은 수를 보고 정원 축소가 통과하고, 지정 후에는 거절되어야 한다.
3. 3번 적용 후 task-114의 기존 시나리오가 모두 통과하는지 확인한다.
4. 1번과 2번은 **변경을 임시로 되돌린 상태에서 새 테스트가 실패하는지** 확인한다. 실패가 재현되지 않으면 진단이 틀렸다는 뜻이므로 보고한다.
5. 결과를 이 문서의 「Result」 절에 기록한다.

`@JpaRepositoryTest`는 클래스 레벨 `@Transactional`이라 쓸 수 없다. `@SpringBootTest` + `@Import(TestContainerConfig.class)`로 구성하고, 스레드·래치 골격은 기존 테스트를 따른다.

## Result

- 2026-09-11 `reschedule`은 `findByIdForUpdate`로 Flag를 읽도록 바꿨다. 모집 마감이 먼저 커밋된
  경합에서 일정 변경은 락 시점에 `isRecruiting() == false`인 최신 상태를 읽었다.
- 현재 도메인 정책도 함께 확인했다. 위 일정 변경이 미래 deadline을 포함하면 `reschedule`은 성공하고
  모집 상태는 다시 `RECRUITING`이 된다. 이 동작은 이번 작업에서 바꾸지 않았으며, 일정 변경 이벤트는
  커밋 후 기존 참여자에게 비동기로 발행될 수 있다.
- `modifyFlagCapacity`에만 `READ_COMMITTED`를 지정했다. 락 직전 테스트용 일반 SELECT 뒤 다른 참여가
  커밋되는 경합에서, 실제 인원 2명보다 작은 정원 1명으로의 축소는 `FlagInvalidStatusException`으로
  거절됐다.
- 대조 실행에서 `reschedule`의 잠금과 정원 변경의 격리 지정을 임시 제거하면 새 경합 테스트 두 건이
  모두 실패했다. 일정 변경은 잠금 조회에 도달하지 않았고, 정원 변경은 예외 없이 축소를 통과했다.
  확인 뒤 두 변경을 원복했다.
- `FlagModificationServiceTest` 11건, `FlagParticipationManagerTest` 6건,
  `FlagParticipationLockConcurrencyIntegrationTest` 8건이 모두 통과했다.

## 범위 제외

- 애플리케이션 서비스 재배치. `FlagModificationService`를 성격별로 쪼개는 것은 별개 판단이며, 잠금이 필요한 세 메서드가 이미 한 파일에 있어 실익이 작다.
- `Flag.reschedule`이 마감 이후 상태를 통과시키는 동작의 변경
- `participant_count` 컬럼 도입 등 정원 검사 방식의 구조 변경
- 격리 수준을 전역으로 바꾸는 것
- `findByIdForUpdate` 포트 이름 변경

## 작업 착수 시

- Domain Change: [x] (3번이 도메인 서비스 내부 구조를 바꾼다)
- `harness/WORKFLOW.md`에 따라 코드 탐색 후 루트 `PLAN.md`에 구현 계획을 작성한다.

## 참고

- `harness/tasks/task-114-flag-participation-lock-concurrency-test.md`
- `harness/tasks/task-115-flag-participation-capacity-isolation.md`
- `src/main/java/com/example/DunbarHorizon/flag/application/service/flag/FlagModificationService.java`
- `src/main/java/com/example/DunbarHorizon/flag/domain/flag/FlagParticipationManager.java`
- `src/test/java/com/example/DunbarHorizon/flag/domain/flag/FlagParticipationLockConcurrencyIntegrationTest.java`
