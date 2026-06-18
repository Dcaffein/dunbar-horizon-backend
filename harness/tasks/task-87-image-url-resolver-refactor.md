# Task-87: 이미지 URL 해석 책임 분리 리팩토링

## 배경

프로필 이미지 업로드는 프론트엔드가 백엔드로부터 S3 presigned PUT URL과 object key를 발급받아
S3에 직접 업로드하는 방식으로 동작한다. 백엔드는 object key만 보관했다가
조회 요청이 오면 key로 presigned GET URL을 로컬 서명 연산(HMAC)으로 생성해 반환한다.

이 설계 의도는 올바르나, 구현 과정에서 두 가지 문제가 발생했다.

---

## 현황 분석

### 문제 1 — Neo4j SocialUser 노드에 만료 URL이 저장됨

프로필 업데이트 시 `UserOutboxDomainEventListener.onUserProfileUpdated()`가
key를 presigned GET URL로 변환한 뒤 `UserSyncIntegrationEvent`에 담아 Neo4j sync를 트리거한다.
결과적으로 Neo4j의 `SocialUser` 노드에는 1시간 만료 URL이 저장된다.

소셜 도메인의 모든 프로필 응답은 이 노드를 직접 읽으므로,
업데이트 후 1시간이 지나면 친구 목록·네트워크 뷰 등 소셜 API에서 프로필 이미지가 깨진다.

```
[MySQL] profileImage = "profiles/uuid-key"   ← key 저장 (올바름)
[Neo4j] profileImageUrl = "https://s3.../presigned?X-Amz-Expires=3600..."  ← 만료 URL (문제)
```

영향 범위: `SocialProfileResult`, `FriendshipDetailResult`, `FriendProfileInfo` 등
소셜 도메인에서 `UserReference.getProfileImageUrl()`을 호출하는 모든 응답.

---

### 문제 2 — presigned URL 생성 로직이 두 어댑터에 중복

동일한 S3 GET presign 로직이 두 곳에 존재한다.

```java
// account/adapter/out/imageStorage/ProfileImageS3Adapter.java
private String resolveUrl(String key) { ... }

// buzz/adapter/out/imageStorage/S3StorageAdapter.java
private String resolveUrl(String key) { ... }
```

두 구현은 `https://` guard 포함 완전히 동일하다.
이후 소셜 도메인에도 같은 로직이 필요해져 세 번째 중복이 발생할 상황이다.

---

## 목적

1. **Neo4j에 key만 저장** — 조회 시점에 URL 생성, 만료 문제 해소
2. **presigned URL 생성 로직 단일화** — `global/imageStorage/S3ImageUrlResolver`로 추출
3. **소셜 도메인에 port/adapter 패턴 적용** — `ImageUrlResolverPort` → `ImageUrlResolverAdapter` → `S3ImageUrlResolver` 위임 구조로 통일

---

## 설계 결정

### global/imageStorage 패키지

`global`은 도메인이 아니므로 port/adapter 쌍을 두지 않는다.
공용 S3 인프라 구현체(`S3ImageUrlResolver`)만 `@Component`로 등록한다.
기존 `global/model/`에 있던 S3 관련 모델(`PresignedUploadResult`, `PresignRequest`)도 이 패키지로 이동한다.

### 소셜 도메인의 URL 해석 구조

각 도메인은 자신의 포트를 정의하고 어댑터가 글로벌 구현체에 위임하는 hexagonal 패턴을 따른다.

```
social/application/port/out/ImageUrlResolverPort  ← 도메인 포트 (인터페이스)
social/adapter/out/imageStorage/ImageUrlResolverAdapter  ← 어댑터 (S3ImageUrlResolver 위임)
global/imageStorage/S3ImageUrlResolver  ← 공용 인프라 구현체
```

account·buzz 도메인도 동일한 방향으로 교체된다.

---

## 변경하지 않는 것

- S3 presigned URL의 TTL (1시간 유지)
- presigned PUT URL 발급 흐름 (프론트 직접 업로드 방식)
- `UserSyncIntegrationEvent` 레코드 구조 (profileImageUrl 필드명 유지, 의미만 key로 변경)
- Neo4j `SocialUser` 노드의 `profileImageUrl` 프로퍼티명 (Neo4j 스키마 마이그레이션 불필요)

---

## 결과 및 디버그 기록

### 구현 완료 항목

| 항목 | 파일 |
|------|------|
| S3 자격증명 명시적 주입 (`StaticCredentialsProvider`) | `global/config/S3Config.java` |
| URL 해석 공용 컴포넌트 추출 | `global/imageStorage/S3ImageUrlResolver.java` |
| S3 모델 패키지 이동 (`global/model` → `global/imageStorage`) | `PresignedUploadResult`, `PresignRequest` |
| 소셜 도메인 포트/어댑터 추가 | `ImageUrlResolverPort`, `ImageUrlResolverAdapter` |
| `SocialProfileResult`, `FriendshipDetailResult` — 조회 시 URL 생성 | resolver 주입 방식으로 변경 |
| `UserOutboxDomainEventListener` — raw key 전달로 수정 | presign 호출 제거 |
| Neo4j sync용 raw key 반환 메서드 추가 | `UserQueryUseCase.getUserProfilesForSync()` |
| `SocialUserSyncHelper` — raw key 저장으로 변경 | `getUserProfilesForSync()` 사용 |
| 만료된 legacy presigned URL 자동 재서명 | `S3ImageUrlResolver.extractKeyAndPresign()` |

### 디버그: 프로필 이미지 업데이트 후 친구 목록에 이미지 미표시

**증상**: `GET /api/v1/users/me` → 이미지 정상 반환, `GET /api/v1/friends/{id}` → `friendProfileImageUrl: null`

**원인 분석 과정**:

1. 서버 로그 확인 → MySQL UPDATE는 발생했으나 outbox INSERT가 없음
2. `UserSyncIntegrationEvent`가 발행되지 않아 `SocialUserEventListener`가 호출되지 않음
3. 이벤트가 발행되지 않는 이유: `UserProfileUpdateService.updateProfile()`이 JPA dirty checking에만 의존하고 `userRepository.save(user)`를 호출하지 않음

**근본 원인**: Spring Data JPA의 `@DomainEvents`는 `SimpleJpaRepository.save()` 내부에서 `publisher.publishEvents(entity)`를 호출할 때만 동작한다. `save()`를 건너뛰면 Hibernate는 dirty checking으로 UPDATE를 실행하지만 도메인 이벤트는 아무도 발행하지 않는다.

**수정**: `UserProfileUpdateService.updateProfile()` 에 `userRepository.save(user)` 추가

```java
// 수정 전
userRepository.findById(userId).orElseThrow(...).updateProfile(nickname, profileImageKey);

// 수정 후
User user = userRepository.findById(userId).orElseThrow(...);
user.updateProfile(nickname, profileImageKey);
userRepository.save(user);  // @DomainEvents 발행 트리거
```

**함께 제거한 잘못된 접근**: 읽기 경로에서 null profileImageUrl을 감지해 MySQL 재sync하는 로직을 추가했다가 제거. 읽기 경로에 sync를 두는 것은 설계 원칙 위반이며 이벤트 경로를 신뢰해야 한다.
