# Task 26: 이메일 인증 토큰 저장소를 MySQL → Redis로 교체

## Objective
현재 MySQL 테이블(`email_verification_tokens`)에 저장 중인 이메일 인증 토큰을 Redis로 이전한다.
24시간짜리 임시 토큰은 Redis TTL로 만료를 자동 처리할 수 있어, 별도 테이블과 만료 체크 로직이 불필요해진다.

## Background

### 현재 구조의 문제
`EmailVerificationToken`은 JPA `@Entity`로 MySQL에 저장된다.

```java
// EmailVerificationToken: TTL 24시간짜리 임시 데이터가 영속 테이블에 저장됨
private static final long EMAIL_TOKEN_EXPIRATION_TIME_VALUE = 24L;
```

**재발송 흐름 (`VerificationService.sendVerificationEmail`):**
```java
verificationTokenRepository.deleteByUser(user);   // DELETE 쿼리
verificationTokenRepository.flush();               // flush (deleteByUser 후 save 충돌 방지)
EmailVerificationToken newToken = new EmailVerificationToken(user);
verificationTokenRepository.save(newToken);        // INSERT 쿼리
```
- DELETE → flush → INSERT 3단계가 필요하며, flush를 빠뜨리면 unique 충돌 가능성이 있다.

**인증 흐름 (`VerificationService.verifyEmail`):**
```java
token.isExpired()  // DB에서 꺼낸 뒤 Java에서 만료 체크
verificationTokenRepository.delete(token);         // 인증 완료 후 DELETE
```
- 만료 여부를 Java 레벨에서 직접 체크해야 한다.

### Redis로 교체했을 때의 이점
- TTL 자동 만료 → `isExpired()` 로직 불필요
- 재발송 시 flush 없이 덮어쓰기(SETEX)로 원자적 처리
- `email_verification_tokens` 테이블과 JPA 엔티티 제거 가능

## Redis 키 설계

| 키 | 값 | TTL |
|----|-----|-----|
| `account:email-verification:{token}` | `userId` | 24시간 |
| `account:email-verification:user:{userId}` | `token` | 24시간 |

- 토큰으로 userId 조회 (인증 흐름)
- userId로 기존 토큰 조회 후 삭제 (재발송 시 기존 토큰 무효화)

## Domain Change
[x] 있음  [ ] 없음
- `EmailVerificationTokenRepository` 포트 인터페이스 변경 (메서드 시그니처 정리)
- `EmailVerificationToken` JPA 엔티티 제거
- Redis 어댑터 신규 추가

## Decision

### 변경 대상

**제거:**
- `EmailVerificationToken` (JPA 엔티티)
- `EmailVerificationTokenJpaRepository`
- `EmailVerificationTokenRepositoryAdapter` (JPA 기반)

**수정:**
- `EmailVerificationTokenRepository` 포트 — Redis 기반에 맞게 메서드 재정의
  ```java
  void save(Long userId, String token);       // SETEX × 2 (token→userId, user→token)
  Optional<Long> findUserIdByToken(String token);  // GET account:email-verification:{token}
  void deleteByUserId(Long userId);           // 기존 토큰 무효화 (재발송 시)
  ```
- `VerificationService` — `flush()` 제거, `isExpired()` 제거, 포트 메서드 변경에 맞게 수정

**신규:**
- `EmailVerificationTokenRedisAdapter` — `RedisTemplate`으로 위 포트 구현

### 체크리스트
- [ ] `EmailVerificationTokenRepository` 포트 메서드 재정의
- [ ] `EmailVerificationTokenRedisAdapter` 구현 (RedisTemplate 사용, TTL 24시간)
- [ ] `VerificationService` 수정 — flush/isExpired 제거, 새 포트 메서드로 교체
- [ ] `EmailVerificationToken` 엔티티, JPA Repository, JPA 어댑터 제거
- [ ] DB 마이그레이션: `email_verification_tokens` 테이블 DROP
- [ ] `VerificationServiceTest` 수정

---

## Result

**브랜치:** `ai/refactor-redis-email-verification`
**커밋:** `c26aa5a`

### 변경 내용

#### 제거
- `EmailVerificationToken` JPA 엔티티
- `EmailVerificationTokenJpaRepository`
- `EmailVerificationTokenRepositoryAdapter` (JPA 기반)

#### 포트 재정의 (`EmailVerificationTokenRepository`)
```java
void save(Long userId, String token);
Optional<Long> findUserIdByToken(String token);
void deleteByUserId(Long userId);
```

#### Redis 어댑터 신규 (`EmailVerificationTokenRedisAdapter`)
- `StringRedisTemplate` 사용, TTL 24시간
- 이중 키 구조: `account:email-verification:{token}` → userId, `account:email-verification:user:{userId}` → token
- `deleteByUserId`: user→token 역방향 조회 후 양쪽 키 삭제

#### VerificationService 단순화
- `sendVerificationEmail`: `deleteByUser` + `flush` → `deleteByUserId` 단일 호출, `UuidUtil.createV7()`로 토큰 생성
- `verifyEmail`: `isExpired()` 제거 (Redis TTL 만료 = 키 없음 = `orElseThrow`), `token.getUser()` → `userRepository.findById(userId)`

#### DB
- `email_verification_tokens` 테이블은 배포 후 수동 `DROP TABLE` 필요

### 테스트 결과
- `VerificationServiceTest`: 4/4 PASSED
  - `sendVerificationEmail` — 정상 발송, 이미 인증된 이메일 예외
  - `verifyEmail` — 정상 인증, 만료/없는 토큰 예외
