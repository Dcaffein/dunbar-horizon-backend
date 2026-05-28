# Task-52: FlagInvitationManager Host 초대 버그 수정

## 배경

`FlagInvitationManager.invite()`에서 초대자(inviter) 권한을 `FlagParticipant` 조회로만 검증한다.
Host는 `FlagParticipant` 테이블에 등록되지 않으므로 `FlagParticipantNotFoundException`이 발생해
Host 본인이 초대를 보낼 수 없는 버그가 있다.
`grantInvitePermission` / `revokeInvitePermission` 자체가 Host 전용 API인데,
정작 Host는 초대를 보낼 수 없는 모순이다.

## 목표

- Host(`flag.getHostId() == inviterId`)이면 `FlagParticipant` 조회 및 `canInvite` 검증을 건너뛰고 초대를 허용한다
- 참여자의 초대 권한 검증(`canInvite`)은 Host가 아닌 경우에만 적용한다

## 수정 대상

`FlagInvitationManager.invite()` — inviter 권한 검증 분기 추가

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

## 브랜치

`ai/refactor-flag-invitation-manager`
