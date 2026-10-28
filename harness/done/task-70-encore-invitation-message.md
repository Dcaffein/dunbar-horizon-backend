# Task 70: 앵콜 초대장 알림 메시지 차별화

## 배경

task-69에서 앵콜 생성 시 부모 참여자에게 `FlagInvitation`을 자동 발송하도록 구현했다.
그런데 앵콜로 발송된 초대장도 일반 초대장과 동일하게 `FLAG_INVITATION` 알림을 사용하기 때문에,
수신자는 "이 초대장이 앵콜이다"라는 사실을 알 수 없다.

앵콜 초대는 이미 함께했던 사람들에게 다시 모임을 제안하는 것이므로,
"새 모임에 초대받았습니다"보다 "함께했던 모임의 앵콜에 초대받았습니다" 형태의 메시지가
수신자에게 훨씬 친숙하고 수락 동기를 높인다.

## 목표

- 앵콜로 발송된 초대장 알림과 일반 초대장 알림의 메시지를 구분한다.
- `FlagInvitation` 엔티티나 DB 스키마는 변경하지 않는다.
- 이벤트 레벨에서 `isEncore` 플래그를 추가하는 것으로 충분하다.

## 설계 결정

### 스키마 미변경 (Option A 채택)

`FlagInvitation`에 `parentFlagId` 컬럼을 추가하면 API 응답에서도 앵콜 여부를 노출할 수 있지만,
현재 프론트에서 초대장 카드에 부모 모임 정보를 보여줄 계획이 없으므로 과설계다.
알림 메시지 구분만 필요하므로 이벤트 필드 추가(Option A)로 충분하다.

### 변경 범위

| 파일 | 변경 내용 |
|------|---------|
| `FlagInvitationSentEvent` | `isEncore` boolean 필드 추가 |
| `FlagInvitationEventListener` | `isEncore` 여부에 따라 알림 제목·본문 분기 |
| `FlagEncoreInvitationListener` | 이벤트 발행 시 `isEncore = true` 전달 |
| `FlagInvitationService` | 일반 초대 이벤트 발행 시 `isEncore = false` 전달 |

## Result

(미완료)
