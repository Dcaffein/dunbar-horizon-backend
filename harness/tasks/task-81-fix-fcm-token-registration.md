# Task-81: 알림 디바이스 토큰 API 신설 및 FCM 발송 오류 처리

## 배경 및 목적

로컬 디버깅 중 FCM 알림 미전송 문제를 조사했다.  
이벤트 체인은 정상이나, stale 토큰 누적으로 실제 수신이 불가한 상태였다.

핵심 결정: **멀티 디바이스 지원 대신 "마지막으로 알림을 ON한 디바이스 1대만 수신"** 정책.  
프론트엔드는 현재 접속 기기의 FCM 토큰을 서버에 조회해 등록 여부를 확인하고,  
사용자가 알림 토글을 켜면 upsert, 끄면 userId 기준 삭제한다.

---

## API 스펙

### 1. 알림 등록 상태 조회

```
GET /api/v1/notifications/device-token/status?token={fcmToken}
Auth: access_token cookie
```

**백엔드 동작:**
- `device_token` 테이블에서 `user_id = 현재유저 AND fcm_token = {token}` 행이 있는지 확인
- 읽기 전용 — 쓰기 없음

**Response:**
```json
{ "registered": true | false }
```

---

### 2. 알림 등록 (Toggle ON)

```
POST /api/v1/notifications/device-token
Auth: access_token cookie
Body: { "token": "fcmToken" }
```

**백엔드 동작:**
- `user_id` 기준 upsert — 이미 토큰이 있으면 교체, 없으면 삽입
- 동일 토큰 재등록이면 no-op
- INSERT가 아닌 upsert 필수 (stale 토큰 누적 방지)

**Response:** `200 OK`

---

### 3. 알림 해제 (Toggle OFF)

```
DELETE /api/v1/notifications/device-token
Auth: access_token cookie
```

**백엔드 동작:**
- `user_id` 기준으로 해당 유저의 토큰 전체 삭제
- 토큰 값 불필요

**Response:** `200 OK`

---

### 4. FCM 발송 오류 처리 (내부)

- FCM이 `UNREGISTERED` / `INVALID_ARGUMENT` 반환 시 해당 토큰 자동 삭제
- stale 토큰이 DB에 남지 않도록 필수

---

## 핵심 제약사항

| 항목 | 요구사항 |
|------|---------|
| 유저당 토큰 수 | 1개 (upsert) |
| status 조회 | 읽기 전용, user_id + 토큰 일치 여부만 확인 |
| DELETE | user_id 기준 (토큰 값 불필요) |
| FCM 오류 | 자동 토큰 삭제 처리 |

---

## Out of Scope

- 멀티 디바이스 지원 (device_id 기반 관리)
- 기존 누적 stale 토큰 DB 정리 — 배포 후 수동 처리
