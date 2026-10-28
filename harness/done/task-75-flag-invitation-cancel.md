# Task-75: Flag 초대 취소 및 수락/거절 후 초대 삭제

## 배경 및 목적

현재 Flag 초대 흐름에 두 가지 문제가 있다.

1. **취소 API 없음**: 초대를 보낸 사람(inviter)이 보낸 초대를 취소할 수단이 없다.
2. **수락/거절 후 행 유지**: 수락(`ACCEPTED`)·거절(`REJECTED`) 처리 후에도 `flag_invitations` 행이 남아 있다. UX상 처리가 끝난 초대는 사라지는 것이 자연스럽고, DB에 상태 행을 축적할 필요가 없다.

## 의도

- 초대를 보낸 사람(inviter)은 아직 응답되지 않은(`PENDING`) 초대를 취소할 수 있어야 한다.
- 초대를 받은 사람(invitee)이 수락하거나 거절하면 해당 초대 행은 DB에서 삭제된다.
- 거절 후 동일 invitee에게 재초대는 허용한다 (행이 없으므로 `existsPendingByFlagIdAndInviteeId` 체크를 통과).
- 수락 후 재초대는 `isParticipating` 체크로 이미 막힌다.

## Out of Scope

- 초대 이력 조회 API (감사 로그 목적)
- 거절 후 재초대 금지 정책 (필요 시 별도 태스크)
