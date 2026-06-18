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
