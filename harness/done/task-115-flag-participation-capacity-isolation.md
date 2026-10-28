# Task-115: Flag 참여 정원 불변식이 깨지는 문제 수정

> 상태: 완료 (2026-09-10)

## 목적

정원이 한 자리 남은 Flag에 두 요청이 동시에 들어오면 **둘 다 성공해 정원을 초과한다.** 일반 참여와 초대 수락 양쪽 모두 해당한다. task-114의 통합 테스트로 재현·확정된 실제 버그이며, 이 작업에서 수정한다.

## 원인 (task-114에서 확정)

MySQL InnoDB의 기본 격리 수준 `REPEATABLE READ`에서, **트랜잭션의 첫 일반 SELECT가 읽기 뷰(read view)를 확정**한다. 이후 그 트랜잭션의 모든 일반 SELECT는 테이블과 무관하게 그 시점의 가시성 판정을 재사용한다. 잠금 읽기(`SELECT ... FOR UPDATE`)는 최신 행을 읽지만 읽기 뷰를 갱신하지 않는다.

참여 확정 경로는 락을 잡기 **전에** 일반 SELECT를 한 번 수행한다. 그래서 이런 순서가 된다.

```
T2: 락 전 일반 SELECT     ← 읽기 뷰 확정 (참여자 0명)
T1: 락 획득 → 참여 → 커밋   (참여자 1명)
T2: findByIdForUpdate     ← 잠금 읽기. Flag 행은 최신
T2: countParticipants     ← 일반 SELECT. 확정된 뷰의 0명을 본다
T2: 정원 통과 → 참여        ← 정원 1인데 2명
```

락은 정상 동작한다. 직렬화도 된다. 낡는 것은 **락 이후의 일반 SELECT 결과**다.

락 전 일반 SELECT의 정체는 경로마다 다르다.

| 경로 | 락 전 일반 SELECT | 읽는 테이블 |
| --- | --- | --- |
| 일반 참여 | `FlagParticipationManager.participate`의 `findHostIdById` | `flags` |
| 초대 수락 | `FlagInvitationManager.updateStatus`의 `invitationRepository.findById` | `flag_invitations` |

**다른 테이블을 읽어도 발생한다.** 읽기 뷰는 테이블 단위가 아니라 트랜잭션 단위이기 때문이다.

### 확정 근거

task-114에서 네 실험으로 원인을 격리했다.

- `SELECT @@transaction_isolation` → `REPEATABLE-READ`
- 참여 서비스에만 임시 `READ_COMMITTED` 적용 → 정원 경합 통과
- `findHostIdById`를 락 뒤로 임시 이동 → 정원 경합 통과
- 락 전 일반 SELECT가 없는 호출(도메인 메서드 직접 호출) → `REPEATABLE-READ`에서도 통과

## 결정: 참여를 확정하는 트랜잭션의 격리 수준을 `READ_COMMITTED`로 지정한다

**이 트랜잭션은 직렬화를 락에서 얻는다.** 격리 수준의 반복 읽기 보장에 기대는 부분이 없고, 오히려 그 보장이 "락을 잡았으니 최신을 보겠다"는 요구와 충돌한다. 격리를 낮추는 것이 아니라 이 경로에 맞는 수준을 고르는 것이다.

락 전 조회를 트랜잭션 밖으로 빼는 방식은 채택하지 않는다. 진입 경로가 둘이고 초대 수락은 `flagId`를 알기 위해 초대를 먼저 읽어야 하므로, 그 방식은 "모든 진입 경로에서 락 전에 아무것도 읽지 않는다"는 규칙이 되어 경로가 늘 때마다 재감사해야 한다. 그 규칙은 호출 그래프를 따라가야 위반이 보인다.

### 적용 지점

메서드 단위로 지정한다. 두 서비스 모두 클래스 레벨 `@Transactional`이 걸려 있으나, 같은 클래스의 다른 메서드(`leaveFlag`, `invite`, `delete`, `updateInvitePermission`)까지 바꿀 이유가 없다.

- `FlagParticipationService.participateInFlag`
- `FlagInvitationCommandService.updateStatus`

초대 쪽은 초대 상태 변경과 참여 확정이 한 트랜잭션이어야 하므로(참여가 실패하면 초대가 남아야 한다) 바깥 트랜잭션에 지정한다.

**두 지점 모두 코드 주석으로 근거를 남긴다.** 근거 없이 격리 수준만 낮춰 두면 나중에 되돌려질 수 있다.

## 반드시 다룰 경계 조건

- **격리 수준은 트랜잭션이 새로 시작될 때만 적용된다.** 이 메서드들이 이미 열린 트랜잭션 안에서 호출되는 경로가 없는지 확인한다. 있으면 지정이 무시되거나 예외가 난다.
- **참여자 삭제는 Flag 락을 잡지 않는다.** `unparticipate`는 `findById` 후 삭제한다. 따라서 `countParticipants`와 참여자 INSERT 사이에 다른 트랜잭션의 탈퇴가 끼어들 수 있다. 이 경우 실제보다 많이 세어 참여를 거절하게 되는데, 정원 초과는 아니므로 안전 방향이다. 현재도 같으므로 이 작업에서 바꾸지 않되, 판단 결과를 기록한다.
- **`READ COMMITTED`에서는 갭 락이 거의 걸리지 않는다.** 이 경로가 갭 락에 의존하는 지점이 없는지 확인한다. `findByIdForUpdate`는 PK 단일 행이고 `flag_participants`에는 유니크 제약이 없어 문제가 없을 것으로 보이나, 확인한다.
- 이 변경은 JPA(MySQL) 트랜잭션 매니저에만 해당한다. Neo4j 경로에는 영향이 없어야 한다.

## 선행 작업: 초대 경로 테스트를 실제 유스케이스로 고친다

task-114의 초대 대조군은 `participateByInvitation`을 도메인 메서드로 직접 호출해 통과했을 가능성이 크다. 그 호출에는 락 전 일반 SELECT가 없다.

**수정 전에** `FlagInvitationCommandService.updateStatus`를 통과하는 정원 경합 테스트를 추가하고, 현재 구현에서 **실패하는지** 확인한다. 실패하지 않으면 초대 경로 진단이 틀린 것이므로 보고한다.

## 검증

1. 선행 작업의 초대 경로 테스트가 수정 전에 실패한다.
2. 격리 수준 지정 후, task-114의 네 테스트와 초대 경로 테스트가 모두 통과한다.
3. 격리 수준 지정을 임시로 되돌리면 정원 경합 테스트 둘이 다시 실패한다. 확인 후 원복한다.
4. 결과를 이 문서의 「Result」 절에 기록한다.

## Result

- 2026-09-10 실제 `FlagInvitationCommandService.updateStatus`를 경유하는 초대 수락 경합 테스트를 추가했다.
  두 요청이 초대장을 일반 조회한 뒤 함께 참여 확정으로 진행하게 고정했으며, 변경 전에는 두 요청이 모두
  성공해 `successCount = 2`로 실패했다.
- `FlagParticipationService.participateInFlag`와 `FlagInvitationCommandService.updateStatus`에만
  `READ_COMMITTED`를 적용한 뒤, task-114의 기존 다섯 시나리오와 실제 초대 수락 경합을 포함한 여섯 시나리오가
  모두 통과했다.
- 메서드 수준 격리 지정을 임시 제거한 대조 실행에서는 일반 참여 정원 경합과 실제 초대 수락 정원 경합이 모두
  `successCount = 2`로 다시 실패했다. 검증 뒤 `READ_COMMITTED` 지정과 근거 주석을 원복했다.

## 범위 제외

- `participant_count` 컬럼 도입 등 정원 검사를 원자적 UPDATE로 바꾸는 구조 변경. 락이 병목으로 실측되면 그때 검토한다.
- 비관적 락 제거
- `(flag_id, participant_id)` 유니크 제약 추가
- `findByIdForUpdate` 포트 이름 변경
- 다른 도메인·다른 경로의 격리 수준 조정
- 전역 격리 수준 변경
- 로컬·운영 DB의 binlog 형식 확인 및 변경. 중단 배포 방식의 토이 프로젝트 범위에서는 MySQL 8의 기본 `ROW` 설정을 전제로 하며, 애플리케이션·복제 설정은 변경하지 않는다.

## 참고

- `harness/tasks/task-114-flag-participation-lock-concurrency-test.md` — 재현 테스트와 원인 확정 기록
- `src/main/java/com/example/DunbarHorizon/flag/domain/flag/FlagParticipationManager.java`
- `src/main/java/com/example/DunbarHorizon/flag/domain/invitation/FlagInvitationManager.java` — `updateStatus`
- `src/main/java/com/example/DunbarHorizon/flag/application/service/flag/FlagParticipationService.java`
- `src/main/java/com/example/DunbarHorizon/flag/application/service/invitation/FlagInvitationCommandService.java`
