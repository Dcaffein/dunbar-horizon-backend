# Task-114: Flag 참여 비관적 락의 동시성 통합 테스트

## 목적

Flag 참여 경로의 비관적 락이 실제로 최신 상태를 읽는다는 것을 통합 테스트로 검증한다.

`FlagParticipationManager`는 정원 불변식을 지키기 위해 `findByIdForUpdate`로 Flag 행에 배타 락을 건다. 그런데 이 경로에는 락 획득 전에 저장소 조회가 하나 있다. 호스트와 신청자가 친구인지 Neo4j에 물으려면 `hostId`가 필요하기 때문이다.

과거 이 조회는 `findById`로 Flag 엔티티 전체를 로드했다. 그 결과 엔티티가 영속성 컨텍스트에 올라갔고, 이후 `findByIdForUpdate`가 `SELECT ... FOR UPDATE`를 실행하고도 **DB에서 읽은 값을 버리고 이미 관리 중인 인스턴스를 반환**했다. 영속성 컨텍스트는 한 트랜잭션 안에서 같은 식별자에 같은 인스턴스를 보장하며, 관리 중인 엔티티의 필드를 쿼리 결과로 덮어쓰지 않는다. 행은 잠겼지만 검증에 쓰인 값은 락 이전 상태였다.

커밋 `346f048`에서 이 조회를 `findHostIdById` 스칼라 프로젝션으로 교체해 해결했다. 엔티티가 컨텍스트에 올라가지 않으므로 락 시점의 조회가 실제 조회가 된다.

**현재 이 동작을 검증하는 테스트가 없다.** 참여 로직 테스트는 열 개 있지만 전부 단일 스레드이고 저장소가 Mockito 목이라, 영속성 컨텍스트가 존재하지 않는 환경에서 돈다. 같은 종류의 테스트를 더 추가해도 이 문제는 잡히지 않는다.

## 반드시 먼저 알아야 할 것: 한 자리 경합은 회귀 테스트가 되지 못한다

"두 트랜잭션이 마지막 한 자리를 두고 경합한다"는 시나리오는 **정상 동작한다.** 먼저 락을 얻은 쪽이 참여를 마치고 커밋하면, 나중 트랜잭션은 락을 얻어 최신 참여자 수를 세고 정원 초과로 거절된다.

문제는 이 시나리오가 **고치기 전 구현에서도 똑같이 통과한다**는 것이다. 선행 조회가 `findById`든 `findHostIdById`든 결과가 같으므로, 회귀 테스트로서는 아무것도 증명하지 못한다.

그 경로에서 쓰이는 값 중 낡을 수 있는 것이 실제로는 달라지지 않기 때문이다.

```java
int count = flagRepository.countParticipants(flagId);   // 쿼리. 영속성 컨텍스트를 거치지 않는다
return lockedFlag.participate(userId, count);
```

```java
if (!this.isRecruiting()) throw ...            // this.schedule  → 낡을 수 있다
if (this.capacity != null && currentCount >= this.capacity) throw ...
//     ↑ this.capacity 낡을 수 있다   ↑ 파라미터. 낡지 않는다
```

`count`는 쿼리라 항상 최신이다. `schedule`과 `capacity`는 낡을 수 있지만, 참여 대 참여 경합에서는 **아무도 그 값을 바꾸지 않으므로** 낡은 값과 최신 값이 같다.

따라서 버그를 드러내려면 경합의 상대가 달라야 한다. 참여와 참여가 아니라, **참여와 호스트의 마감·정원 변경**이 부딪히는 형태여야 한다.

## 작성할 테스트

### (1) 락 시점의 fresh read 보장 — 좁은 테스트

저장소 수준에서 명제 하나만 검증한다. **프로젝션으로 선행 조회를 하면, 락 조회가 최신 값을 읽는다.**

- 트랜잭션 A: `findHostIdById(flagId)` 호출
- 그사이 별도 트랜잭션 B가 해당 Flag의 `schedule`(또는 `capacity`)을 변경하고 커밋
- 트랜잭션 A: `findByIdForUpdate(flagId)` 호출 → **B가 바꾼 값이 보여야 한다**

`findById`로 선행 조회했을 때 낡은 값이 보인다는 것까지 테스트로 못 박지는 않는다. 그것은 우리 코드가 아니라 Hibernate의 동작이며, 버전에 따라 달라질 수 있는 것을 고정하게 된다.

### (2) 참여 흐름의 동시성 회귀 방어 — 넓은 테스트

실제 참여 유스케이스에서 검증한다.

- 모집 중인 Flag에 사용자가 참여를 시도한다
- 참여 흐름이 락을 획득하기 **전**에, 호스트가 `closeRecruitment()`로 모집을 마감하고 커밋한다
- 참여 시도는 `FlagDeadlinePassedException`으로 거절되어야 한다

참여 흐름을 락 획득 직전에 멈추는 제어가 필요하다. 프로덕션 코드에 훅을 넣지 말고, 테스트 설정에서 `FlagRepository`를 감싸 선행 조회 후 대기시키는 스파이 빈을 등록하는 방식을 검토한다.

### (3) 초대 경로 대조군

`participateByInvitation`은 락 앞 선행 조회가 없어 이 문제가 구조적으로 발생할 수 없다. 같은 시나리오에서도 정상 거절되는지 확인해 대조를 남긴다.

### (4) 정원 불변식의 동시성 보장

위 세 가지와 달리 이 버그와는 무관한, 별개 명제를 검증한다. **락이 정원을 지킨다.**

- 정원이 한 자리 남은 Flag에 두 사용자가 동시에 참여를 시도한다
- 하나만 성공하고 나머지는 `FlagFullCapacityException`으로 거절되어야 한다
- 최종 참여자 수가 정원을 넘지 않아야 한다

락을 도입한 이유가 정원인데 이를 검증하는 테스트가 현재 하나도 없다. 스파이 훅 없이 두 스레드가 `participate`를 호출하면 되므로 넷 중 가장 간단하다. 이 테스트는 옛 구현에서도 통과하므로 검증 절차 3번의 대상이 아니다.

## 결정사항 및 제약

- **프로덕션 코드는 변경하지 않는다.** 이 작업은 기존 동작의 검증이다.
- `@JpaRepositoryTest`는 사용할 수 없다. 클래스 레벨 `@Transactional`이 붙어 있어 테스트 전체가 한 트랜잭션으로 묶이는데, 이 검증은 서로 다른 트랜잭션 둘을 요구한다. `harness/TESTING-GUIDE.md`의 "JPA 테스트는 `JpaRepositoryTest`를 상속" 규칙에 대한 예외이며, 그 사유를 테스트 클래스 주석에 남긴다.
- `@SpringBootTest` + `@Import(TestContainerConfig.class)`로 구성한다. 같은 패키지의 `FlagConclusionEventIntegrationTest`, `FlagRescheduleEventIntegrationTest`가 선례다.
- 스레드와 래치 골격은 `social/adapter/out/neo4j/FriendRequestConcurrencyTest`를 참고한다. 프로젝트에서 유일한 동시성 통합 테스트다.
- 트랜잭션 경계를 테스트가 직접 제어해야 하므로 `TransactionTemplate` 또는 별도 스레드에서의 명시적 커밋을 사용한다.
- `@SpringBootTest`는 롤백되지 않는다. `@AfterEach`에서 생성한 Flag와 참여자를 정리한다.
- Given-When-Then 주석과 한국어 `@DisplayName`을 붙인다.

## 검증 절차

테스트가 통과하는 것만으로는 회귀를 막는다는 증거가 되지 않는다. **작성 후 다음을 확인하고 결과를 이 문서의 「Result」 절에 기록한다.**

1. 현재 구현에서 (1)(2)(3)(4)가 모두 통과하는지 확인한다.
2. `FlagParticipationManager`의 `findHostIdById` 호출을 임시로 `findById`로 되돌리고, `hostId`를 `flag.getHostId()`에서 얻도록 바꾼다.
3. 이 상태에서 (2)가 **실패하는지** 확인한다. 실패해야 정상이다. (1)도 선행 조회를 `findById`로 바꾼 변형에서 실패하는지 함께 본다.
4. 원복하고 전체 테스트가 통과하는지 재확인한다.
5. 3번에서 실패가 재현되지 않으면 **구현을 진행하지 말고 보고한다.** 이 작업의 전제인 진단이 틀렸다는 뜻이다.

## 범위 제외

- 프로덕션 코드의 동작 변경
- `findByIdForUpdate` 포트 이름 변경
- 정원 검사를 원자적 UPDATE나 카운터 컬럼으로 바꾸는 구조 변경
- `(flag_id, participant_id)` 유니크 제약 추가
- 다른 도메인의 동시성 테스트 보강

## Result

- 2026-09-09 현재 구현에서 (1) fresh read, (2) 일반 참여와 마감의 경합, (3) 초대 참여 대조군은 통과했다.
- (4) 정원 한 자리 동시 참여는 실패했다. 두 일반 참여 요청이 모두 성공해 최종 참여자 수가 2가 됐다.
  `findHostIdById`의 비잠금 조회가 MySQL `REPEATABLE READ` 읽기 스냅샷을 먼저 만든 뒤,
  `findByIdForUpdate` 이후의 `countParticipants`가 먼저 커밋한 참여자를 보지 못한 것으로 확인됐다.
- 테스트컨테이너 MySQL의 `SELECT @@transaction_isolation` 결과는 `REPEATABLE-READ`였다.
  `FlagParticipationService`에만 임시로 `READ_COMMITTED`를 적용하면 (4)가 통과했다.
  해당 변경은 진단 직후 원복했다.
- 일반 참여에서 `findHostIdById`를 임시로 없애고, 락을 얻은 Flag의 hostId로 친구 확인을 수행한 변형도
  (4)를 통과했다. 락 앞 첫 일반 SELECT가 사라진 효과이며, 이 변형 역시 원복했다.
- 초대 참여 경로에 정원 한 자리 동시 참여 대조군을 추가한 결과, 같은 `REPEATABLE-READ`에서 성공 1건과
  `FlagFullCapacityException` 1건으로 통과했다. 이 경로는 락 앞 일반 SELECT가 없다.
- 현재 작업은 프로덕션 코드 변경을 범위에서 제외하므로, 기준선 네 시나리오가 모두 통과하지 못한 상태에서
  `findHostIdById`를 옛 `findById` 방식으로 바꾸는 회귀 증명 단계는 실행하지 않았다. 정원 불변식의
  트랜잭션/조회 전략을 별도 결정한 뒤 재개해야 한다.

## 작업 착수 시

- Domain Change: [ ]
- `harness/WORKFLOW.md`에 따라 코드 탐색 후 루트 `PLAN.md`에 구현 계획을 작성한다.
- 이 문서는 후속 작업 의뢰를 위한 기록이며, 구현은 PLAN 승인 후 진행한다.

## 참고

- `346f048` — 1PC 캐시 오염 수정 커밋. 테스트를 변경하지 않았다.
- `d8e5048` (task-27) — 락 보유 중이던 Neo4j 조회를 락 밖으로 옮긴 커밋. 선행 조회가 생긴 원인이다.
- `src/main/java/com/example/DunbarHorizon/flag/domain/flag/FlagParticipationManager.java`
- `src/main/java/com/example/DunbarHorizon/flag/application/service/flag/FlagModificationService.java` — `closeRecruitment`, `modifyFlagCapacity`. 둘 다 같은 락을 잡는다.
