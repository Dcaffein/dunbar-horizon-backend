# Task-79: FCM 멀티 디바이스 알림 구조 개선

## 배경 및 목적

현재 `notification_settings` 테이블은 `user_id`를 PK로 FCM 토큰을 1개만 저장한다.
멀티 디바이스 환경에서 새 기기로 로그인하면 기존 토큰이 덮어씌워져 이전 기기가 알림을 받지 못한다.
또한 로그아웃 시 FCM 토큰을 정리하지 않아 로그아웃한 기기에도 알림이 계속 전달된다.

프론트엔드와 합의된 설계로 백엔드 구조를 개선한다.

## 합의된 설계 원칙

- 알람 ON/OFF 상태의 단일 진실 공급원: `device_tokens` 테이블의 row 존재 여부
- 별도 `isAlarmOn` 필드 없음 — 토큰 row가 있으면 ON, 없으면 OFF
- 각 기기는 독립적인 알람 상태를 가짐
- FCM 토큰 로테이션 감지 및 갱신은 프론트엔드에서 처리

## 변경 사항

### 1. DB 구조 변경

```
(기존) notification_settings
  user_id (PK), fcm_token, isAlarmOn

(변경) device_tokens
  id (PK, auto increment)
  user_id (FK, index)
  fcm_token (unique)
  created_at
```

### 2. API 변경

| 변경 | 엔드포인트 | 설명 |
|------|-----------|------|
| 유지 (수정) | `POST /api/v1/notifications/device-token` | 토큰 등록 (중복 시 무시) |
| 추가 | `DELETE /api/v1/notifications/device-token` | 토큰 삭제 (알람 OFF / 로그아웃) |
| 추가 | `GET /api/v1/notifications/device-token/status?token=` | 현재 기기 토큰 등록 여부 조회 |

로그아웃 API(`DELETE /api/auth/tokens`) body에 `fcmToken`을 선택 필드로 추가하여 로그아웃과 토큰 삭제를 한 번에 처리한다.

### 3. 알림 발송 변경

`NotificationEventListener`에서 수신자 토큰 조회 시 `device_tokens` 테이블에서 `userId`에 해당하는 **모든 토큰**을 조회하여 multicast.

### 4. 죽은 토큰 정리

FCM 응답에서 `UNREGISTERED` / `INVALID_ARGUMENT` 에러 수신 시 해당 토큰 row 삭제 (기존 로직 유지, 테이블만 변경).

## 이벤트별 처리 흐름

| 이벤트 | 처리 |
|--------|------|
| 최초 알람 허용 (브라우저 권한 팝업) | `POST /device-token` → row 추가 |
| 로그아웃 | `DELETE /api/auth/tokens { fcmToken }` → refresh token + token row 삭제 |
| 재로그인 | `POST /device-token` → 기존 토큰 재등록 (중복 무시) |
| 앱 내 알람 OFF | `DELETE /device-token { token }` → row 삭제 |
| 앱 내 알람 ON | `POST /device-token { token }` → row 추가 |
| 브라우저 권한 취소 감지 | 다음 앱 로드 시 getToken() 실패 → `DELETE /device-token` |
| 토큰 로테이션 | 프론트엔드가 감지 → `POST /device-token { newToken }` (기존 토큰 row 교체) |
| 앱 삭제 / 토큰 만료 | FCM UNREGISTERED 응답 → 백엔드 자동 row 삭제 |
| 회원 탈퇴 | 해당 userId의 device_tokens 전체 삭제 |

## Out of Scope

- 브라우저 알림 권한 팝업 처리 (프론트엔드)
- FCM 토큰 로테이션 감지 및 갱신 (프론트엔드)
- 알림 타입별 세분화 설정 (per-type mute)
- 전체 기기 일괄 알람 OFF (글로벌 뮤트)
