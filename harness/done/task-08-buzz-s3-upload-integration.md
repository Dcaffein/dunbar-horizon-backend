## Objective
이미지 URL 문자열만 처리하던 방식에서 실제 파일을 AWS S3에 저장하고 관리하는 인프라 연동 체계를 구축합니다.

## Domain Change
[ ] 없음 — 필드 타입(String URL)은 유지됨

## Dependencies
이 작업을 수행하기 위해 AWS S3 SDK 설정이 선행되어야 합니다.
- build.gradle에 S3 의존성 추가 필요

## Target Files
- build.gradle — AWS S3 SDK 의존성 추가
- buzz/adapter/out/infrastructure/S3StorageAdapter.java — 신규 생성 (S3 연동)
- buzz/application/service/BuzzService.java — 파일 업로드 흐름 추가
- buzz/adapter/in/web/BuzzController.java — Multipart 요청 처리
- buzz/adapter/in/web/dto/BuzzCreateRequest.java — 타입 변경
- buzz/adapter/in/web/dto/BuzzReplyRequest.java — 타입 변경

## Requirements
1. build.gradle 파일에 AWS S3 연동을 위한 software.amazon.awssdk:s3 의존성을 추가합니다.
2. multipart/form-data를 통해 전달된 이미지 파일을 S3에 업로드하고, 생성된 Public URL을 획득하는 어댑터를 구현합니다. (버킷 정보 등은 application.yml 환경변수로 주입)
3. Buzz 및 BuzzReply 생성/수정 시 획득한 S3 URL 문자열을 저장하도록 로직을 수정합니다.
4. 데이터베이스와 이미지 간의 물리적 삭제 불일치(수명 주기 관리)는 인프라 정책으로 위임합니다.

## Decisions
| 항목 | 결정값 | 비고 |
|------|--------|------|
| 데이터 저장 방식 | S3 Public URL (String) | DB 부하 최소화 |
| 통신 프로토콜 | multipart/form-data | 이미지 직접 전송 |

## Out of Scope
- S3 Lifecycle Policy 설정은 인프라 레이어에서 수행

## Testing Strategy
- Unit: S3 어댑터를 Mocking하여 서비스 로직이 도메인 모델에 객체 URL을 정상 할당하는지 검증
- Integration: Testcontainers와 LocalStack을 활용하여 S3 컨테이너를 띄우고, 실제 파일 스트림 업로드 및 응답 URL 생성 과정을 검증

## API Contract Change
[x] 있음
- 변경 전: POST /api/v1/buzzes 및 POST /api/v1/buzzes/{buzzId}/replies (JSON Body: { "imageUrls": ["string"] })
- 변경 후: POST /api/v1/buzzes 및 POST /api/v1/buzzes/{buzzId}/replies (multipart/form-data, key: images, value: File[])