# PLAN: task-10 Buzz Reply → Comment 리팩토링

## [도메인 수정 승인 요청]

`BuzzReply` 클래스 및 `Buzz` 엔티티 내부 필드·메서드명을 Comment 계열로 전면 변경합니다.
도메인 이벤트(`BuzzRepliedEvent`)와 관련 값 객체(`BuzzReply`)의 명칭도 함께 변경됩니다.

---

## 목표

Buzz 도메인 내 Reply 계열 용어를 Comment로 통일하여 Flag 도메인과 유비쿼터스 언어를 맞추고, API 경로도 `/replies` → `/comments`로 변경합니다.

---

## 변경 파일 전체 목록

### 도메인 (4개)

| 파일 | 변경 내용 |
|------|---------|
| `buzz/domain/BuzzReply.java` → `BuzzComment.java` | 클래스명, 필드명 전면 변경 |
| `buzz/domain/Buzz.java` | `replies` 필드, `createReply` / `updateReply` / `validateReplyDeletion` 메서드 |
| `buzz/domain/event/BuzzRepliedEvent.java` → `BuzzCommentedEvent.java` | 클래스명, `replierId` → `commenterId` |

### 어플리케이션 (5개)

| 파일 | 변경 내용 |
|------|---------|
| `buzz/application/dto/result/BuzzReplyResult.java` → `BuzzCommentResult.java` | 클래스명, `replyId` 필드 |
| `buzz/application/dto/result/BuzzDetailResult.java` | `replies` → `comments`, `BuzzReplyResult` → `BuzzCommentResult` |
| `buzz/application/port/in/BuzzCommandUseCase.java` | `replyToBuzz` → `commentOnBuzz`, `updateReply` → `updateComment`, `deleteReply` → `deleteComment` |
| `buzz/application/service/BuzzService.java` | 메서드명, 지역 변수명 |
| `buzz/application/eventHandler/BuzzNotificationDispatcher.java` | `BuzzRepliedEvent` → `BuzzCommentedEvent`, `replierId` → `commenterId` |

### 어댑터 (5개)

| 파일 | 변경 내용 |
|------|---------|
| `buzz/adapter/in/web/dto/BuzzReplyRequest.java` → `BuzzCommentRequest.java` | 클래스명 |
| `buzz/adapter/in/web/BuzzController.java` | 경로 `/replies` → `/comments`, DTO·메서드 참조 |
| `buzz/adapter/out/persistence/mongo/BuzzField.java` | `RESPONSES = "replies"` → `RESPONSES = "comments"` |
| `buzz/adapter/out/persistence/mongo/BuzzMongoTemplateRepository.java` | `BuzzReply` → `BuzzComment` 참조 |
| `buzz/adapter/out/persistence/BuzzRepositoryAdapter.java` | 메서드명 |

### 테스트 (3개)

| 파일 | 변경 내용 |
|------|---------|
| `buzz/domain/model/BuzzReplyTest.java` → `BuzzCommentTest.java` | 클래스명, 참조 전면 변경 |
| `buzz/domain/model/BuzzTest.java` | `createReply` → `createComment` 호출부 |
| `buzz/adapter/out/mongo/BuzzMongoTemplateRepositoryTest.java` | `BuzzReply.of` → `BuzzComment.of` |

---

## 명칭 변환 일람

| 기존 | 변경 후 |
|------|---------|
| `BuzzReply` | `BuzzComment` |
| `BuzzRepliedEvent` | `BuzzCommentedEvent` |
| `BuzzReplyResult` | `BuzzCommentResult` |
| `BuzzReplyRequest` | `BuzzCommentRequest` |
| `replies` (필드) | `comments` |
| `replyId` | `commentId` |
| `replierId` | `commenterId` |
| `replierNickname` | `commenterNickname` |
| `replierProfileImageUrl` | `commenterProfileImageUrl` |
| `createReply` / `addReply` | `createComment` / `addComment` |
| `updateReply` | `updateComment` |
| `validateReplyDeletion` | `validateCommentDeletion` |
| `replyToBuzz` | `commentOnBuzz` |
| `/replies` (API 경로) | `/comments` |

---

## 사이드 이펙트

- **MongoDB 필드명 변경**: `BuzzField.RESPONSES`가 `"replies"` → `"comments"`로 바뀌어 기존 저장된 Buzz 문서의 replies 데이터가 매핑되지 않습니다. Buzz TTL이 30분이므로 개발 환경에서는 컬렉션 드롭 후 재시작으로 해결됩니다. 운영 환경 적용 시에는 데이터 마이그레이션이 필요합니다.

---

## 테스트 계획

- 기존 단위 테스트를 Comment 명칭으로 수정 후 컴파일 및 실행 통과 확인
- `BuzzControllerTest` API 경로 `/replies` → `/comments` 수정

---

## 브랜치

`ai/refactor-buzz-comment-rename`
