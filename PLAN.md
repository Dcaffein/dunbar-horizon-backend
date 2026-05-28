# PLAN: Task-52 FlagInvitationManager Host 초대 버그 수정

## 목표

`FlagInvitationManager.invite()`에서 Host가 `FlagParticipant` 레코드 없이도 초대를 보낼 수 있도록 수정한다.
아울러 `accept()` 위임 구조 변경에 맞춰 테스트를 정합한다.

---

## 현황 분석

### 버그
`invite()`의 inviter 권한 검증이 `participantRepository.findByFlagIdAndParticipantId(flagId, inviterId)`에만 의존한다.
Host는 `FlagParticipant` 테이블에 없으므로 `FlagParticipantNotFoundException` 발생 → Host가 초대 불가.

### 테스트 불일치
`FlagInvitationManagerTest`의 `accept_*` 테스트들이 `participantRepository`, `flagRepository`를 직접 스텁하고 있다.
그러나 현재 `accept()`는 이 로직을 `FlagParticipationManager`에 위임하므로 `@Mock FlagParticipationManager`가 없어 실행 시 NPE 발생.

---

## 변경 파일 목록

| 파일 | 변경 내용 |
|------|-----------|
| `flag/domain/flag/FlagParticipationManager.java` | `participateByInvitation()` 추가, `updateCapacity()` 제거 — 검증 로직이 `Flag.updateCapacity()` 도메인 메서드에 이미 있어 불필요 |
| `flag/domain/flag/FlagInvitationManager.java` | `invite()` — inviterId가 hostId이면 Participant 조회·canInvite 검증 skip |
| `flag/domain/flag/FlagInvitationManagerTest.java` | `@Mock FlagParticipationManager` 추가, `accept_*` 스텁 교체, `invite_HostIsInviter_Success` 테스트 추가 |

---

## 구현 방향

### FlagInvitationManager.invite()

```java
boolean inviterIsHost = flag.getHostId().equals(inviterId);
if (!inviterIsHost) {
    FlagParticipant inviter = participantRepository
        .findByFlagIdAndParticipantId(flagId, inviterId)
        .orElseThrow(() -> new FlagParticipantNotFoundException(inviterId));
    if (!inviter.isCanInvite()) {
        throw new FlagAuthorizationException("초대 권한이 없습니다.");
    }
}
```

### FlagInvitationManagerTest

- `@Mock FlagParticipationManager flagParticipationManager` 추가
- `accept_Success`: `participantRepository`·`flagRepository` 스텁 제거 → `given(flagParticipationManager.participateByInvitation(...)).willReturn(participant)` 으로 교체
- `accept_AlreadyParticipating_Throws`: 위임 후 예외 발생이므로 `given(flagParticipationManager.participateByInvitation(...)).willThrow(FlagParticipationDuplicateException.class)` 로 교체
- `invite_HostIsInviter_Success` 추가: inviterId = HOST_ID, participantRepository 스텁 없이 성공

---

## 예상 사이드 이펙트

없음. 호스트가 초대자인 경우는 기존에 예외로 터지던 경로이므로 기존 동작에 영향 없음.
