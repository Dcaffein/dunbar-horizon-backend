# PLAN.md — Task 04: Social Profile Lazy Sync 안전망

## 현재 상태 분석

Lazy Sync의 핵심 흐름은 **이미 구현**되어 있다:

```
SocialUserService.getUserReference(userId)
  → getUserReferences() : Neo4j에 없으면 missingIds 식별
    → SocialUserSyncHelper.syncAndSave(missingIds)
      → userQueryUseCase.getUserProfiles()  ← Account에서 조회
      → socialUserRepository.saveAll()      ← Neo4j에 저장
```

Account 모듈에도 없으면 `UserReferenceNotFoundException (404)` 이미 발생 → AC 2, 4 충족.

## 미충족 항목 (실제 작업 범위)

| Acceptance Criteria | 현재 상태 | 작업 필요 |
|---------------------|-----------|-----------|
| AC1. Neo4j Unique Constraint | ❌ 없음 | 추가 필요 |
| AC2. 없으면 Account에서 조회 | ✅ 구현됨 | — |
| AC3. MERGE 쿼리로 생성 | ⚠️ save()는 SDN 내부적으로 MERGE 사용 | 명시적 처리로 변경 |
| AC4. Account에도 없으면 404 | ✅ 구현됨 | — |
| AC5. DataIntegrityViolationException 처리 | ❌ 없음 | 추가 필요 |

## 아키텍처 위반 수정

`SocialUserSyncHelper`(social **application** 계층)가 `UserQueryUseCase`(account **application** 계층)를 직접 참조 → **계층 경계 위반**.

ARCHITECTURE.md 규칙: `port/out` 인터페이스를 정의하고 `adapter/out`에서 구현.

```
[현재] social/application/service/SocialUserSyncHelper → account.application.port.in.UserQueryUseCase (위반)

[변경] social/application/service/SocialUserSyncHelper → social/application/port/out/UserProfilePort (준수)
                                                             ↑ implements
       social/adapter/out/AccountUserProfileAdapter → account.application.port.in.UserQueryUseCase (adapter에서 참조 허용)
```

---

## 생성/수정 파일 목록

### 신규 생성 (3개)

| 파일 경로 | 역할 |
|-----------|------|
| `social/application/port/out/UserProfilePort.java` | Output Port 인터페이스 (getActiveUserProfile, getUserProfiles) |
| `social/adapter/out/AccountUserProfileAdapter.java` | UserProfilePort 구현, UserQueryUseCase 위임 |
| `social/adapter/out/neo4j/schema/SocialUserSchemaInitializer.java` | 앱 기동 시 Neo4j Unique Constraint 생성 (`@PostConstruct` + `Neo4jClient`) |

### 수정 (1개)

| 파일 경로 | 변경 내용 |
|-----------|---------|
| `social/application/service/SocialUserSyncHelper.java` | `UserQueryUseCase` → `UserProfilePort`로 교체, `DataIntegrityViolationException` catch-and-ignore 추가 |

> **MERGE 처리**: Spring Data Neo4j의 `save()`는 사용자 지정 ID 엔티티에 대해 내부적으로 MERGE를 사용한다. Unique Constraint와 DataIntegrityViolationException catch 추가로 AC3, AC5를 충족한다. 별도 `@Query("MERGE ...")` 메서드는 추가하지 않는다.

---

## 테스트 계획

| 테스트 클래스 | 유형 | 검증 항목 |
|--------------|------|---------|
| `SocialUserSyncHelperTest` | 단위 (Mockito) | 미싱 유저 조회 → Port 호출 → 저장, DataIntegrityViolationException 시 정상 반환 |

---

## 예상 사이드 이펙트

1. **Neo4j Unique Constraint**: 이미 중복 id 노드가 존재한다면 Constraint 생성 시 실패한다. 신규 환경 기준으로 구현한다.
2. **`SocialUserSyncHelper` 변경**: `UserQueryUseCase` 의존성 제거로 기존 테스트가 있다면 수정이 필요하다. (`SocialUserSyncHelper` 전용 테스트는 현재 없음 — 확인 완료)

---

## 브랜치

`ai/feat-sync-outbox` (Task 03 브랜치에서 이어서 작업)
