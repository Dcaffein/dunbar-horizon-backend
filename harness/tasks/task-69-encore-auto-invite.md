# Task 69: Flag 앵콜 시 부모 참여자 자동 초대

## 배경

Flag 앵콜(encore)은 종료된 모임을 같은 멤버들과 다시 여는 흐름이다.
현재 앵콜이 생성되면 부모 Flag 참여자들에게 푸시 알림만 발송한다.
사용자는 알림을 받고 앵콜 Flag를 직접 찾아 참여 신청해야 하는데,
이 과정이 번거롭고 참여 전환율이 낮다.

앵콜의 맥락상 부모 참여자들은 이미 모임의 신뢰 관계가 형성된 멤버들이다.
앵콜 생성 시 이들에게 자동으로 초대장을 발송하면, 수락 한 번으로 재참여가 가능해진다.
기존 `FlagInvitation` 메커니즘을 재사용하므로 도메인 일관성도 유지된다.

## 목표

- 앵콜 Flag 생성 직후, 부모 Flag의 참여자 전원에게 `FlagInvitation`을 자동 생성한다.
- 기존 초대 흐름(`FlagInvitationSentEvent` → 알림)을 그대로 타서 초대 알림이 발송된다.
- 중복 초대 / 이미 참여 중인 경우 / 앵콜 호스트 본인은 제외한다.

## 도메인 변경

[ ] 없음 [x] 있음

- `FlagInvitationRepository` 포트에 `saveAll`, `findPendingInviteeIdsByFlagId` 추가
  - 앵콜 시 참여자 N명에게 벌크 초대장을 생성해야 하므로 단건 `save` 루프 대신 벌크 경로 신설

## 설계 결정

### 신규 리스너 분리

기존 `FlagEncoreEventListener`는 알림 발송 책임을 갖는다.
초대장 생성은 별도 관심사이므로 `FlagEncoreInvitationListener`를 새로 만든다.
동일한 `FlagEncoreEvent`를 구독하되 `AFTER_COMMIT` + `@Async`로 실행한다.

### encore flag ID 조회

`FlagEncoreEvent`에는 encore flag ID가 없다 (이벤트 등록 시점에 JPA ID 미확정).
`flagRepository.findByParentId(parentFlagId)`로 커밋 후 조회한다.

### 중복 방지

| 조건 | 처리 |
|------|------|
| 앵콜 호스트 본인 | 초대 대상에서 제외 |
| 이미 앵콜에 참여 중 | 제외 (엣지케이스, 앵콜 직후에는 사실상 없음) |
| 이미 pending 초대 존재 | 제외 (재시도 안전) |
| 앵콜이 모집 중이 아닌 경우 | 전체 스킵 |

### 초대 만료 시각

`FlagInvitation.expiresAt` = 앵콜 Flag의 `schedule.deadline` 그대로 사용.
기존 단건 초대와 동일한 정책.

## 변경 파일 목록

| 파일 | 변경 내용 |
|------|---------|
| `flag/application/eventListener/FlagEncoreInvitationListener.java` | 신규 — 앵콜 이벤트 수신 후 벌크 초대장 생성 |
| `flag/domain/invitation/repository/FlagInvitationRepository.java` | `saveAll`, `findPendingInviteeIdsByFlagId` 추가 |
| `flag/adapter/out/persistence/FlagInvitationRepositoryAdapter.java` | 위 메서드 위임 구현 |

`FlagInvitationJpaRepository`는 `JpaRepository` 상속으로 `saveAll` 내장.
`findPendingInviteeIdsByFlagId`는 메서드 이름 쿼리로 추가.

---

## Result

- `FlagEncoreInvitationListener` 신규 생성. `FlagEncoreEvent` AFTER_COMMIT + `@Async` + `REQUIRES_NEW` 트랜잭션으로 실행.
- `FlagEncoreEventListener`에서 `FLAG_ENCORE` 알림 발행 제거 — `FLAG_INVITATION` 알림으로 통합.
- `FlagInvitationRepository` 포트에 `saveAll`, `findPendingInviteeIdsByFlagId` 추가 및 어댑터 위임 구현.
- `FlagEncoreInvitationListenerTest` 7케이스 단위 테스트 작성 (정상·호스트 제외·pending 중복·참여 중 중복·참여자 없음·모집 중 아님·encore 없음).
- 커밋: `3a28621` → main 머지 완료 (`ai/feat-encore-auto-invite`)
