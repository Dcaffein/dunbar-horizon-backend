# PLAN: 이미지 URL 해석 책임 분리 리팩토링

> 배경 및 목적: `harness/tasks/task-87-image-url-resolver-refactor.md` 참조

## 브랜치

`ai/refactor-image-url-resolver` (main 분기)

---

## Phase 1: `global/imageStorage` 패키지 신설

### 1-1. `global/model/` 파일 이동

| 이전 위치 | 이후 위치 |
|-----------|-----------|
| `global/model/PresignedUploadResult.java` | `global/imageStorage/PresignedUploadResult.java` |
| `global/model/PresignRequest.java` | `global/imageStorage/PresignRequest.java` |

패키지명만 변경 (`global.model` → `global.imageStorage`).

영향 파일 (import 수정):
- `account/adapter/in/web/AccountController.java`
- `buzz/adapter/in/web/BuzzController.java`
- `account/adapter/out/imageStorage/ProfileImageS3Adapter.java`
- `buzz/adapter/out/imageStorage/S3StorageAdapter.java`

### 1-2. `global/imageStorage/S3ImageUrlResolver.java` 신규

```java
@Component
public class S3ImageUrlResolver {
    private static final Duration PRESIGN_TTL = Duration.ofHours(1);

    private final S3Presigner s3Presigner;
    private final String bucket;

    public String resolveUrl(String key) {
        if (key == null) return null;
        if (key.startsWith("https://")) return key; // 레거시 URL 방어
        // S3Presigner로 presigned GET URL 생성
    }

    public List<String> resolveUrls(List<String> keys) {
        return keys.stream().map(this::resolveUrl).toList();
    }
}
```

---

## Phase 2: 기존 도메인 어댑터를 S3ImageUrlResolver에 위임

### 2-1. `account/adapter/out/imageStorage/ProfileImageS3Adapter.java`

`resolveUrl()` 내부 구현 제거 → `s3ImageUrlResolver.resolveUrl(key)` 위임.

### 2-2. `buzz/adapter/out/imageStorage/S3StorageAdapter.java`

`resolveUrl()`, `resolveUrls()` 내부 구현 제거 → `s3ImageUrlResolver.resolveUrl/resolveUrls()` 위임.

---

## Phase 3: Neo4j에 key 저장 (이벤트 리스너 수정)

### 3-1. `account/application/eventListener/UserOutboxDomainEventListener.java`

`onUserProfileUpdated()`에서 presigned URL 변환 제거. raw key를 그대로 전달.

**변경 전:**
```java
String resolvedUrl = event.profileImageUrl() != null
        ? profileImageStoragePort.resolveUrl(event.profileImageUrl())
        : null;
// resolvedUrl을 payload와 UserSyncIntegrationEvent에 사용
```

**변경 후:**
```java
// resolvedUrl 제거, event.profileImageUrl() (raw key)를 그대로 사용
```

`ProfileImageStoragePort` 의존성 필드 제거.

### 3-2. `UserOutboxDomainEventListenerTest.java`

`ProfileImageStoragePort` mock 제거.

---

## Phase 4: social 도메인 `ImageUrlResolverPort` 신설

### 4-1. `social/application/port/out/ImageUrlResolverPort.java` (신규)

```java
public interface ImageUrlResolverPort {
    String resolveUrl(String key);
}
```

### 4-2. `social/adapter/out/imageStorage/ImageUrlResolverAdapter.java` (신규)

```java
@Component
public class ImageUrlResolverAdapter implements ImageUrlResolverPort {
    private final S3ImageUrlResolver s3ImageUrlResolver;

    @Override
    public String resolveUrl(String key) {
        return s3ImageUrlResolver.resolveUrl(key);
    }
}
```

---

## Phase 5: 소셜 서비스 조회 시 URL 변환 적용

Neo4j에는 key가 저장되므로, 프로필 이미지를 응답에 담는 모든 소셜 서비스에서 `ImageUrlResolverPort`를 통해 변환 필요.

### 대상 서비스 및 수정 방향

| 서비스 | 변경 내용 |
|--------|-----------|
| `SocialUserService` | `ImageUrlResolverPort` 주입, `SocialProfileResult` 생성 시 key → URL 변환 |
| `FriendshipQueryService` | `ImageUrlResolverPort` 주입, `FriendshipDetailResult` 생성 시 변환 |
| `FriendRequestQueryService` | 친구 요청 목록에 프로필 이미지 포함 여부 확인 후 변환 |

### DTO factory method 수정 방향

현재 `SocialProfileResult.from(UserReference ref)`, `FriendProfileInfo.from(UserReference user)`는
`ref.getProfileImageUrl()`을 그대로 담는다 → URL 변환이 필요하므로 서비스 계층에서 직접 생성하거나
factory method에 resolver를 인자로 추가하는 방식 선택.

---

## 변경 파일 요약

| 파일 | 변경 종류 |
|------|-----------|
| `global/model/PresignedUploadResult.java` | 이동 → `global/imageStorage/` |
| `global/model/PresignRequest.java` | 이동 → `global/imageStorage/` |
| `global/imageStorage/S3ImageUrlResolver.java` | **신규** |
| `account/adapter/in/web/AccountController.java` | import 수정 |
| `buzz/adapter/in/web/BuzzController.java` | import 수정 |
| `account/adapter/out/imageStorage/ProfileImageS3Adapter.java` | resolveUrl → 위임 |
| `buzz/adapter/out/imageStorage/S3StorageAdapter.java` | resolveUrl → 위임 |
| `account/application/eventListener/UserOutboxDomainEventListener.java` | URL 변환 제거, Port 의존성 제거 |
| `account/application/eventListener/UserOutboxDomainEventListenerTest.java` | mock 제거 |
| `social/application/port/out/ImageUrlResolverPort.java` | **신규** |
| `social/adapter/out/imageStorage/ImageUrlResolverAdapter.java` | **신규** |
| `social/application/service/SocialUserService.java` | URL 변환 추가 |
| `social/application/service/FriendshipQueryService.java` | URL 변환 추가 |
| `social/application/service/FriendRequestQueryService.java` | URL 변환 여부 확인 후 결정 |
