# PLAN: task-78 — 특정 유저의 최근 Flag 5개 조회 API

## 작업 목표

`GET /api/v1/flags/users/{userId}/recent` — 특정 유저가 호스팅하거나 참여한 Flag 중 `createdAt DESC` 상위 5개 반환

---

## 현황 분석

### 기존 유저 Flag 조회
- `GET /api/v1/flags/users/{userId}?role=HOST|PARTICIPANT` — 역할별 전체 목록 (FlagQueryController)
- `FlagQueryUseCase.getFlagsByRole(userId, role)` — 호스팅 또는 참여 중 하나만 조회

### 신규 요구사항과의 차이
- 호스팅 + 참여 **합산** 필요
- `createdAt DESC` 정렬 + **limit 5** 필요
- 기존 `findAllByHostId`, `findFlagIdsByParticipantId` 조합 시 애플리케이션 레이어 정렬·슬라이싱 발생 → DB 레벨 단일 쿼리로 해결

---

## 변경 파일 목록

| 레이어 | 파일 | 변경 내용 |
|--------|------|---------|
| JPA | `FlagJpaRepository` | JPQL 추가: `hostId = :userId OR id IN (서브쿼리)`, `ORDER BY f.createdAt DESC`, `Pageable` limit |
| Domain Port | `FlagRepository` | `findRecentByUserId(Long userId, int limit)` 추가 |
| Adapter | `FlagRepositoryAdapter` | 위 메서드 `PageRequest.of(0, limit)` 로 위임 |
| UseCase | `FlagQueryUseCase` | `getRecentFlags(Long userId)` 추가 |
| Service | `FlagQueryService` | host 배치 조회 + participantCount 조합, 기존 패턴 동일 |
| Controller | `FlagQueryController` | `GET /users/{userId}/recent` 추가 |

---

## 구현 방향

### JPQL 쿼리
```jpql
SELECT f FROM Flag f
WHERE f.hostId = :userId
   OR f.id IN (SELECT fp.flagId FROM FlagParticipant fp WHERE fp.participantId = :userId)
ORDER BY f.createdAt DESC
```
- `DISTINCT` 불필요 (host이면서 participant인 경우 없음)
- soft-delete: 기존 쿼리들이 `deletedAt` 명시 필터 없이 Hibernate `@SQLRestriction` 또는 `@Where` 에 위임하는 패턴과 동일하게 처리

### Service 구현 패턴
기존 `getFlagsByRole` → `getHostingFlags` 패턴 동일:
1. `flagRepository.findRecentByUserId(userId, 5)`
2. `flagUserPort.findUserInfosByIds(hostIds)` 배치 조회
3. `flagRepository.countParticipantsByFlagIds(flagIds)`
4. `FlagResult.of(...)` 조합

---

## 예상 사이드 이펙트

- 없음. 기존 엔드포인트와 path 충돌 없음 (`/users/{userId}` vs `/users/{userId}/recent`)

---

## 테스트 전략

- `FlagQueryServiceTest`: `getRecentFlags` 단위 테스트 (host+participant 합산, limit 5 검증)
- `FlagQueryControllerTest`: `GET /api/v1/flags/users/{userId}/recent` 200 응답 검증
