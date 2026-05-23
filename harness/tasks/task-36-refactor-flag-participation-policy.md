# Task-36: FlagParticipationPolicy 개선

## 배경

현재 `FlagParticipationService.participateInFlag()`는 `findByIdExclusive`로 MySQL 락을 먼저 획득한 뒤
`FlagParticipationPolicy`에서 Neo4j(`friendshipChecker.areFriends`)를 조회한다.
MySQL 락을 보유한 채 Neo4j 트랜잭션을 여는 크로스-DB 락 문제가 있다.

또한:
- `validateParticipationEligibility`가 `participate` 외에 재사용되지 않아 별도 메서드로 분리할 이유가 없다
- `unparticipate`는 Policy를 거치지 않아 `participate`와 비대칭 구조다 (`Flag.unparticipate()`가 public)
- `updateCapacity`는 Policy에 있을 필요 없이 Service에서 직접 처리 가능하다

## 목표

- Policy가 `flagId`를 받아 내부에서 이중 조회(non-exclusive → Neo4j → exclusive)를 직접 관리
- Neo4j 조회 후 트랜잭션 반환, 이후 MySQL 락 획득 순서로 크로스-DB 락 제거
- `validateParticipationEligibility` 인라인 → `participate` 메서드에 직접 통합
- `unparticipate`를 Policy로 이동, `Flag.unparticipate()` package-private으로 변경
- `updateCapacity` Policy에서 제거 → `FlagManagementService`로 이동

## 변경 흐름

### participate
```
Policy.participate(flagId, userId)
  1. flagRepository.findById(flagId)           → hostId 확보 (락 없음)
  2. friendshipChecker.areFriends(hostId, userId) → Neo4j 조회, 트랜잭션 반환
  3. flagRepository.findByIdExclusive(flagId)  → MySQL 락 획득
  4. flagParticipantRepository.isParticipating(...)  → 중복 체크
  5. flagParticipantRepository.countByFlagId(...)
  6. flag.participate(userId, count)           → 도메인 로직
```

### unparticipate
```
Policy.unparticipate(flagId, userId)
  1. flagRepository.findById(flagId)
  2. flagParticipantRepository.findByFlagIdAndParticipantId(...)
  3. flag.unparticipate(participant, userId)   → package-private 호출
```

## 변경 파일

| 파일 | 변경 내용 |
|------|----------|
| `flag/domain/flag/FlagParticipationPolicy.java` | flagId 수신, 이중 조회, 인라인, unparticipate 추가, updateCapacity 제거, FlagRepository 추가 |
| `flag/domain/flag/Flag.java` | `unparticipate()` public → package-private |
| `flag/application/service/flag/FlagParticipationService.java` | Policy 호출 단순화 |
| `flag/application/service/flag/FlagManagementService.java` | `updateCapacity` 로직 직접 처리 |

## 체크리스트

- [ ] `FlagParticipationPolicy` — flagId 파라미터, 이중 조회, 인라인, unparticipate 추가, updateCapacity 제거
- [ ] `Flag.unparticipate()` — package-private으로 변경
- [ ] `FlagParticipationService` — participate/unparticipate 모두 Policy 경유
- [ ] `FlagManagementService` — updateCapacity 직접 처리
- [ ] 테스트 코드 업데이트
- [ ] 빌드 확인

## 브랜치

`ai/refactor-flag-participation-policy`

## Result

- 브랜치: `ai/refactor-flag-participation-policy`
- 커밋: `1d8d054`
- 변경 파일:
  - `flag/domain/flag/FlagParticipationPolicy.java` — `flagId` 수신, 이중 조회(non-exclusive → Neo4j → exclusive), `validateParticipationEligibility` 인라인, `unparticipate` 추가, `updateCapacity` 제거, `FlagRepository` 추가
  - `flag/domain/flag/Flag.java` — `unparticipate()` public → package-private, `updateCapacity()` package-private → public
  - `flag/application/service/flag/FlagParticipationService.java` — Policy 단일 창구 위임, `FlagRepository` 의존성 제거
  - `flag/application/service/flag/FlagManagementService.java` — `updateCapacity` 직접 처리, `FlagParticipationPolicy` 의존성 제거, `FlagParticipantRepository` 추가
  - `FlagParticipationServiceTest.java` — Policy 위임 검증으로 단순화
  - `FlagManagementServiceTest.java` — Policy mock 제거, `modifyFlagCapacity` 테스트 추가
  - `FlagParticipationPolicyTest.java` — 신규 생성 (비즈니스 규칙 전체 검증)
