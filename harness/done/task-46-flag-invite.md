# Task-46: Flag 초대 기능

## 배경

현재 Flag 참여는 호스트와 친구 관계인 사용자만 가능하다. 참여자가 자신의 다른 친구(호스트와는 친구가 아닐 수 있는)를 모임에 초대하는 경로가 없다. 호스트가 특정 참여자에게 초대 권한을 부여하면, 해당 참여자가 자신의 친구를 초대할 수 있게 한다.

## 목표

- 호스트가 참여자에게 초대 권한을 부여/회수할 수 있다
- 초대 권한을 가진 참여자가 자신의 친구에게 초대장을 보낼 수 있다
- 초대받은 사람은 수락/거절할 수 있으며, 수락 시 호스트와의 친구 여부 검증을 우회하고 참여자가 된다
- 초대는 Flag가 모집 중(`RECRUITING`) 상태일 때만 가능하다

## 도메인 설계

### FlagParticipant — `canInvite` 필드 추가
```java
private boolean canInvite = false;
```
호스트만 `grantInvitePermission()` / `revokeInvitePermission()` 호출 가능.

### FlagInvitation — 신규 엔티티
```
flagId | inviterId | inviteeId | status(PENDING/ACCEPTED/REJECTED) | expiresAt
```
- 초대는 만료 시간을 가진다 (Flag 마감일 기준)
- 동일 (flagId, inviteeId) 쌍의 PENDING 초대는 중복 생성 불가

### 참여 권한 검증 분기
기존 `FlagParticipationPolicy.participate()` → 친구 검증 포함  
초대 수락 경로 → `FlagInvitation`의 유효성만 검증, 친구 여부 우회

## 주요 API

```
PATCH  /api/v1/flags/{flagId}/participants/{participantId}/invite-permission
       body: { canInvite: true/false }   # 호스트 전용

POST   /api/v1/flags/{flagId}/invitations
       body: { inviteeId }               # 초대 권한 보유 참여자 전용

POST   /api/v1/flag-invitations/{invitationId}/accept
POST   /api/v1/flag-invitations/{invitationId}/reject
```

## 결정 필요 사항

- 초대받은 사람이 호스트와 친구가 아니어도 참여 가능한가? → **가능** (초대의 의미)
- 초대받은 사람이 이미 참여자인 경우 → 중복 참여 예외
- 초대받은 사람이 호스트인 경우 → 예외

## 브랜치

`ai/feat-flag-invite`

## 결과

- `FlagParticipant`에 `canInvite` 필드 추가, `grantInvitePermission()` / `revokeInvitePermission()` 메서드 추가
- `FlagInvitation` 엔티티 신규 생성 (status: PENDING/ACCEPTED/REJECTED, expiresAt)
- `FlagInvitationPolicy` 도메인 정책 클래스 신규 생성 — `updateInvitePermission`, `invite`, `accept`, `reject` 로직 담당
- `FlagInvitationService` 유스케이스 신규 생성 — 정책 위임 후 저장 및 이벤트 발행
- 초대 수락 시 `Flag.participate()`를 통해 친구 여부 우회하여 참여자 등록
- `FlagInvitationEventListener` 신규 생성 — 초대 발송 시 `FLAG_INVITATION` 푸시 알림 발행
- `FlagInvitationPolicyTest` (12개), `FlagInvitationServiceTest` (3개) 단위 테스트 작성
