# Task-84: 플래그 초대 목록 조회 API 추가

## 배경

`/api/v1/flag-invitations`에는 수락/거절/취소 명령 API만 존재하고, 사용자가 자신이 받은 초대 또는 보낸 초대를 조회하는 API가 없다.
프론트엔드에서 초대함/보낸 초대 화면을 구성하려면 목록 조회 API가 필요하다.

---

## 구현할 API

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/v1/flag-invitations/received` | 현재 로그인 사용자가 받은 초대 목록 |
| GET | `/api/v1/flag-invitations/sent` | 현재 로그인 사용자가 보낸 초대 목록 |

---

## 현재 구조 파악

### 도메인
- `FlagInvitation`: `id`, `flagId`, `inviterId`, `inviteeId`, `status(PENDING/ACCEPTED/REJECTED)`, `expiresAt`, `createdAt`
- `FlagInvitationStatus`: PENDING, ACCEPTED, REJECTED (확인 필요)

### 기존 레이어
- `FlagInvitationController` — accept/reject/cancel 엔드포인트만 존재
- `FlagInvitationUseCase` (port/in) — 명령 메서드만 존재
- `FlagInvitationRepository` (port/out) — `findById`, `existsPendingByFlagIdAndInviteeId`, `findPendingInviteeIdsByFlagId` 등 조회용 메서드 부재
- `FlagInvitationJpaRepository` — 목록 조회 메서드 없음
- `FlagInvitationService` — 쿼리 로직 없음

---

## 구현 방향

### 추가할 레이어 (헥사고날 구조 준수)

```
FlagInvitationController          ← GET /received, GET /sent 엔드포인트 추가
  └── FlagInvitationQueryUseCase  ← 신규 port/in 인터페이스
        └── FlagInvitationQueryService  ← 신규 서비스
              └── FlagInvitationRepository  ← 신규 메서드 추가
                    └── FlagInvitationJpaRepository  ← 신규 JPA 쿼리
```

### 응답 DTO

`FlagInvitationResult` 신규 생성 (`application/dto/result/`):
- `id: Long`
- `flagId: Long`
- `inviterId: Long`
- `inviteeId: Long`
- `status: FlagInvitationStatus`
- `createdAt: LocalDateTime`
- `expiresAt: LocalDateTime`

### 포트/레포지토리 추가 메서드

`FlagInvitationRepository`:
```java
List<FlagInvitation> findReceivedByInviteeId(Long inviteeId);
List<FlagInvitation> findSentByInviterId(Long inviterId);
```

`FlagInvitationJpaRepository`:
```java
List<FlagInvitation> findAllByInviteeId(Long inviteeId);
List<FlagInvitation> findAllByInviterId(Long inviterId);
```

---

## 변경하지 않는 것

- 기존 `FlagInvitationUseCase` 인터페이스 (명령 전용으로 유지)
- 기존 `FlagInvitationService` 내부 로직
- `FlagInvitation` 도메인 엔티티

---

## 브랜치

`ai/feat-flag-invitation-list-api`
