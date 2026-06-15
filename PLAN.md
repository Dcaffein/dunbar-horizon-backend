# PLAN: FCM 멀티 디바이스 알림 구조 개선 (task-79)

## 작업 목표

`notification_settings` (1유저 1토큰) 구조를 `device_tokens` (1유저 N토큰)으로 교체한다.
토큰 row 존재 여부가 알람 ON/OFF의 단일 진실 공급원이 되며, `isAlarmOn` 필드를 제거한다.

---

## 현황 분석

| 항목 | 현재 상태 | 문제 |
|------|----------|------|
| `NotificationSetting` | `userId` PK + `fcmToken` 1개 | 멀티 디바이스 시 마지막 토큰으로 덮어씌워짐 |
| `isAlarmOn` | 항상 `true`로 하드코딩 생성 | dead field, 불일치 상태 가능 |
| 로그아웃 | refresh token만 삭제 | FCM 토큰이 DB에 남아 로그아웃 기기에도 알림 전달 |
| `DeviceTokenRegisteredEvent` | `isAlarmOn` 포함 | 불필요한 필드 |
| `subscribeToTopic` | `isAlarmOn=true`일 때만 구독 | 토큰 등록 = 항상 구독으로 단순화 가능 |

JPA `ddl-auto: update` → 새 entity 추가 시 테이블 자동 생성, 기존 `notification_settings` 테이블은 수동 정리 필요 (로컬 개발 환경).

---

## 변경 파일 목록

### 삭제 (신규 파일로 대체)
- `notification/domain/NotificationSetting.java`
- `notification/domain/repository/NotificationSettingRepository.java`
- `notification/adapter/out/persistence/NotificationSettingRepositoryAdapter.java`
- `notification/adapter/out/persistence/jpa/NotificationSettingJpaRepository.java`

### 생성
| 파일 | 내용 |
|------|------|
| `notification/domain/DeviceToken.java` | `id(PK)`, `userId`, `fcmToken(unique)`, `BaseTimeEntity` 상속 |
| `notification/domain/repository/DeviceTokenRepository.java` | port interface |
| `notification/adapter/out/persistence/DeviceTokenRepositoryAdapter.java` | adapter |
| `notification/adapter/out/persistence/jpa/DeviceTokenJpaRepository.java` | Spring Data JPA repo |
| `notification/adapter/in/web/dto/DeviceTokenStatusResponse.java` | `{ registered: boolean }` |
| `account/adapter/in/web/dto/LogoutRequest.java` | `{ fcmToken: String }` (optional body) |
| `global/event/DeviceTokenDeregisteredEvent.java` | `record(String fcmToken)` |

### 수정
| 파일 | 변경 내용 |
|------|----------|
| `notification/domain/event/DeviceTokenRegisteredEvent.java` | `isAlarmOn` 필드 제거 |
| `notification/application/NotificationService.java` | `DeviceTokenRepository`로 교체, `removeDeviceToken()` / `isTokenRegistered()` 추가 |
| `notification/application/NotificationEventListener.java` | 토큰 조회 로직 교체, `isAlarmOn` 필터 제거, `DeviceTokenDeregisteredEvent` 핸들러 추가 |
| `notification/adapter/in/web/NotificationController.java` | `DELETE /device-token`, `GET /device-token/status` 추가 |
| `account/adapter/in/web/AccountController.java` | 로그아웃 body에 optional `fcmToken` 수신 |
| `account/application/port/in/LoginUseCase.java` | `logout(refreshToken, fcmToken)` 시그니처 변경 |
| `account/application/service/LoginService.java` | `ApplicationEventPublisher` 추가, fcmToken 있으면 `DeviceTokenDeregisteredEvent` 발행 |

---

## 구현 방향

### 1. DeviceToken 엔티티
```java
@Entity @Table(name = "device_tokens")
public class DeviceToken extends BaseTimeEntity {
    @Id @GeneratedValue
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(unique = true, nullable = false)
    private String fcmToken;
}
```

### 2. 토큰 등록 (`POST /device-token`)
- 동일 토큰이 이미 존재하면 무시 (`existsByFcmToken` 체크 후 early return)

### 3. 알람 상태 조회 (`GET /device-token/status?token=`)
- `DeviceTokenRepository.existsByFcmToken(token)` → `{ registered: true/false }`

### 4. 토큰 삭제 (`DELETE /device-token`)
- body: `{ token: "..." }` → `DeviceTokenRepository.deleteByFcmToken(token)` + FCM 토픽 해지

### 5. 로그아웃 시 토큰 연계
- `AccountController.logout()`: optional `@RequestBody LogoutRequest(fcmToken)`
- `LoginService.logout(refreshToken, fcmToken)`: fcmToken 있으면 `DeviceTokenDeregisteredEvent` 발행
- `NotificationEventListener.handleDeviceTokenDeregistered()`: `notificationService.removeDeviceToken(fcmToken)`
- 도메인 간 직접 참조 없이 event로 처리 → account / notification 결합 없음
- 핸들러에 `@TransactionalEventListener(phase = BEFORE_COMMIT)` 사용 → 외부 트랜잭션 커밋 직전 실행, 실패 시 로그아웃 전체 롤백 (원자성 보장)

### 6. 알림 발송 토큰 조회
- 기존: `settingRepository.findAllByUserIdIn()` + `isAlarmOn` 필터
- 변경: `deviceTokenRepository.findAllTokensByUserIdIn()` (토큰 row 존재 = 알람 ON)

### 7. 죽은 토큰 정리
- `UNREGISTERED` / `INVALID_ARGUMENT` 에러 → `deviceTokenRepository.deleteAllByFcmTokenIn(invalidTokens)`

### 8. notice 토픽 구독
- 기존: `isAlarmOn=true`일 때만 구독
- 변경: 토큰 등록 시 무조건 구독 (`DeviceTokenRegisteredEvent`에서 `isAlarmOn` 제거)

---

## API 변경 요약

| 변경 | 엔드포인트 | 설명 |
|------|-----------|------|
| 유지 (내부 수정) | `POST /api/v1/notifications/device-token` | 토큰 등록 |
| 추가 | `DELETE /api/v1/notifications/device-token` | 토큰 삭제 (알람 OFF) |
| 추가 | `GET /api/v1/notifications/device-token/status?token=` | 토큰 등록 여부 조회 |
| 수정 | `DELETE /api/auth/tokens` | body에 optional fcmToken 추가 |

---

## 예상 사이드 이펙트

- `NotificationSetting` 참조 전체 제거 → 관련 import 교체 필요
- `notification_settings` 테이블은 `ddl-auto: update`로 자동 삭제되지 않음 → 로컬 DB 수동 drop 필요
- 기존 토큰 데이터 마이그레이션 없음 (개발 단계, 데이터 초기화 허용)

---

## 테스트 전략

- `NotificationService` 단위 테스트: 토큰 등록(중복), 삭제, 상태 조회
- `NotificationEventListener` 단위 테스트: `DeviceTokenDeregisteredEvent` 처리
- `LoginService` 단위 테스트: fcmToken 있을 때 / 없을 때 이벤트 발행 여부
