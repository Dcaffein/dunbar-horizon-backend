# Task-49: S3 Pre-signed URL 전환

## 배경

현재 이미지 업로드는 클라이언트가 서버에 `multipart/form-data`로 파일을 전송하고, 서버가 파일 전체를 힙에 올린 뒤(`file.getBytes()`) S3에 동기 블로킹 `putObject`로 푸시하는 구조다.

- `S3StorageAdapter`: Buzz 이미지 여러 장을 순차적으로 `file.getBytes()` → `putObject`
- `ProfileImageS3Adapter`: 프로필 이미지를 `file.content()` → `putObject`

서버가 파일 데이터를 버퍼링하므로 메모리 압박이 크고, 업로드 스로틀이 서버 스레드를 점유한다. Pre-signed URL로 전환하면 업로드 경로가 S3로 분리되어 서버는 URL 생성과 key 저장만 담당한다.

---

## 목표

- S3 버킷을 프라이빗으로 유지한다
- **업로드**: 서버가 presigned PUT URL을 발급 → 클라이언트가 S3에 직접 PUT
- **조회**: 서버가 응답 조립 시 objectKey로 presigned GET URL을 로컬 서명 연산으로 생성 (AWS 네트워크 요청 없음)
- DB에 full URL 대신 **objectKey만 저장**한다

---

## 흐름

### 업로드 (Buzz 이미지)

```
① POST /api/v1/buzzes/images/presign
   body: [{ contentType, size }, ...]
   ← 서버: S3Presigner로 presigned PUT URL + objectKey 생성 (로컬 서명)
   → [{ uploadUrl, objectKey }, ...]

② PUT {uploadUrl} × N  ← 클라이언트가 S3에 직접 업로드

③ POST /api/v1/buzzes
   body: { text, recipientSpec, imageKeys: ["buzz/uuid_photo.jpg", ...] }
   ← 서버: imageKeys 그대로 저장 (HeadObject 검증 없음 — 클라이언트 신뢰)
```

### 업로드 (프로필 이미지)

```
① POST /api/v1/users/me/profile-image/presign
   body: { contentType, size }
   → { uploadUrl, objectKey }

② PUT {uploadUrl}

③ PATCH /api/v1/users/me/profile
   body: { nickname, profileImageKey: "profiles/uuid_photo.jpg" }
```

### 조회

```
GET /api/v1/buzzes/{id}
← 서버: DB에서 imageKeys 조회 → S3Presigner로 presigned GET URL 생성 (TTL 1시간)
→ { imageUrls: ["https://bucket.s3.region.amazonaws.com/buzz/uuid?X-Amz-Signature=..."], ... }
```

---

## 포트 설계

### ImageStoragePort (buzz 도메인)

```java
// 기존 upload() 제거 후 교체
List<PresignedUploadResult> presignUploads(List<PresignRequest> requests);
List<String> resolveUrls(List<String> keys);
```

### ProfileImageStoragePort (account 도메인)

```java
// 기존 upload() 제거 후 교체
PresignedUploadResult presignUpload(String contentType);
String resolveUrl(String key);
```

### 공통 record (global 패키지)

```java
public record PresignRequest(String contentType, long size) {}
public record PresignedUploadResult(String uploadUrl, String objectKey) {}
```

---

## DB 마이그레이션

기존 데이터는 `profileImageUrl`과 `buzz.imageUrls`에 full URL이 저장되어 있다.  
응답 조립 시 값이 `https://`로 시작하면 그대로 내려보내고, 그렇지 않으면 presigned GET URL을 생성하는 폴백을 `resolveUrl` 내부에 적용한다.  
신규 데이터부터 objectKey만 저장한다.

---

## 변경 파일

| 파일 | 변경 내용 |
|------|-----------|
| `S3Config.java` | `S3Presigner` 빈 추가 |
| `global/model/PresignRequest.java` | 신규 — presign 요청 record |
| `global/model/PresignedUploadResult.java` | 신규 — presign 응답 record |
| `ImageStoragePort.java` | `upload()` → `presignUploads()` + `resolveUrls()` |
| `ProfileImageStoragePort.java` | `upload()` → `presignUpload()` + `resolveUrl()` |
| `S3StorageAdapter.java` | presigned PUT URL 발급 + presigned GET URL 생성으로 교체 |
| `ProfileImageS3Adapter.java` | presigned PUT URL 발급 + presigned GET URL 생성으로 교체 |
| `BuzzCommandUseCase.java` | `List<MultipartFile>` 파라미터 → `List<String> imageKeys` |
| `BuzzService.java` | MultipartFile 제거, `imageKeys` 처리, 응답 시 `resolveUrls()` 호출 |
| `BuzzController.java` | presign 엔드포인트 추가, multipart 제거 |
| `UserProfileUpdateUseCase.java` | `UploadFile` 파라미터 → `String profileImageKey` |
| `UserProfileUpdateService.java` | UploadFile 제거, `profileImageKey` 처리, `resolveUrl()` 호출 |
| `AccountController.java` | presign 엔드포인트 추가, multipart 제거 |
| `UploadFile.java` | 삭제 (더 이상 사용하지 않음) |

---

## 브랜치

`ai/feat-s3-presign`
