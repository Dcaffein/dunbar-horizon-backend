# Task 18: Flag 도메인 테스트 추가

## Objective
Flag 도메인 전체(flag, comment, memorial 서브도메인 포함)에 테스트가 전혀 없다. 도메인 엔티티 단위 테스트, 서비스 레이어 단위 테스트(Mockito 기반), 컨트롤러 슬라이스 테스트(@WebMvcTest 기반)를 계층별로 추가하여 핵심 비즈니스 로직과 API 계약을 검증한다.

## Domain Change
[ ] 없음

## Target Files

### 신규 생성 (테스트)
- `src/test/java/com/example/DunbarHorizon/flag/domain/flag/FlagTest.java` — Flag 엔티티 생성/참여/수정/삭제 로직
- `src/test/java/com/example/DunbarHorizon/flag/domain/comment/FlagCommentTest.java` — FlagComment 엔티티 생성/수정/삭제/가시성 로직
- `src/test/java/com/example/DunbarHorizon/flag/application/service/flag/FlagQueryServiceTest.java` — 플래그 조회 서비스
- `src/test/java/com/example/DunbarHorizon/flag/application/service/flag/FlagHostServiceTest.java` — 플래그 생성/앵코르 서비스
- `src/test/java/com/example/DunbarHorizon/flag/application/service/flag/FlagManagementServiceTest.java` — 플래그 관리 서비스
- `src/test/java/com/example/DunbarHorizon/flag/application/service/flag/FlagParticipationServiceTest.java` — 플래그 참여/탈퇴 서비스
- `src/test/java/com/example/DunbarHorizon/flag/application/service/comment/FlagCommentQueryServiceTest.java` — 댓글 조회 서비스
- `src/test/java/com/example/DunbarHorizon/flag/application/service/comment/FlagCommentCommandServiceTest.java` — 댓글 명령 서비스
- `src/test/java/com/example/DunbarHorizon/flag/application/service/memorial/FlagMemorialCommandServiceTest.java` — 추도문 명령 서비스
- `src/test/java/com/example/DunbarHorizon/flag/application/service/memorial/FlagMemorialQueryServiceTest.java` — 추도문 조회 서비스
- `src/test/java/com/example/DunbarHorizon/flag/adapter/in/web/FlagControllerTest.java` — Flag CRUD 컨트롤러 슬라이스 테스트
- `src/test/java/com/example/DunbarHorizon/flag/adapter/in/web/FlagQueryControllerTest.java` — FlagQuery 컨트롤러 슬라이스 테스트
- `src/test/java/com/example/DunbarHorizon/flag/adapter/in/web/FlagCommentControllerTest.java` — 댓글 컨트롤러 슬라이스 테스트
- `src/test/java/com/example/DunbarHorizon/flag/adapter/in/web/FlagMemorialControllerTest.java` — 추도문 컨트롤러 슬라이스 테스트

## Requirements

### 1. 도메인 단위 테스트

**FlagTest**
- `create()`: 유효한 입력으로 플래그 생성 성공
- `createEncore()`: 종료된 플래그(deletedAt 있음)에서 앵코르 생성 성공; 아직 활성 중인 플래그에서 앵코르 시도 시 예외 발생
- `participate()`: 정원 미달 상태에서 참여 성공; 정원 초과 시 `FlagFullCapacityException` 발생; 호스트가 참여 시도 시 예외 발생
- `closeRecruitment()`: 호출 후 마감일이 현재 시각으로 변경됨
- `delete()`: 소프트 삭제 시 `deletedAt`이 설정됨

**FlagCommentTest**
- `createRoot()`: 루트 댓글 생성 성공; 내용이 비어있으면 검증 예외 발생
- `createReply()`: 1단 답글 생성 성공; 2단 이상 중첩 시 `FlagCommentReplyDepthException` 발생
- `update()`: 작성자 본인 수정 성공; 타인이 수정 시도 시 `FlagCommentAuthorizationException` 발생
- `issueDeletionScope()`: 작성자 본인이 요청 시 삭제 스코프 반환; 타인이 요청 시 예외 발생
- `isVisibleTo()`: 공개 댓글은 누구에게나 보임; 비공개 댓글은 호스트와 작성자에게만 보임; 비공개 댓글은 제3자에게 보이지 않음

### 2. 서비스 단위 테스트 (`@ExtendWith(MockitoExtension.class)`)

**FlagQueryServiceTest**
- `getMyHostingFlags()`: 호스팅 플래그 목록 정상 반환
- `getParticipatingFlags()`: 참여 플래그 목록 정상 반환
- `getFlagDetail()`: 플래그 미존재 시 `FlagNotFoundException` 발생; HOST/PARTICIPANT/GUEST 각 역할에 맞는 응답 반환
- `determineViewerRole()`: isHost=true → HOST; isHost=false, isParticipant=true → PARTICIPANT; 둘 다 false → GUEST

**FlagHostServiceTest**
- `hostFlag()`: 정상 플래그 생성 및 저장 검증
- `encoreFlag()`: 부모 플래그 미존재 시 `FlagNotFoundException` 발생; 아직 활성 중인 플래그에서 앵코르 시도 시 예외 발생

**FlagManagementServiceTest**
- `modifyFlagDetails()`: 호스트가 수정 시 성공; 비호스트가 수정 시 `FlagAuthorizationException` 발생
- `modifyFlagCapacity()`: 현재 참여자 수보다 적게 설정 시 예외 발생
- `reschedule()`: 유효하지 않은 일정(시작 > 종료) 시 `FlagScheduleInvalidException` 발생
- `closeRecruitment()`: 이미 모집 종료된 플래그에서 재시도 시 예외 발생
- `closeFlag()`: 호스트가 삭제 시 성공; 비호스트가 삭제 시 `FlagAuthorizationException` 발생

**FlagParticipationServiceTest**
- `participateInFlag()`: 정상 참여 성공; 이미 참여 중인 경우 `FlagParticipationDuplicateException` 발생; 마감일 경과 시 `FlagDeadlinePassedException` 발생; 정원 초과 시 `FlagFullCapacityException` 발생
- `leaveFlag()`: 정상 탈퇴 성공; 참여자 미존재 시 `FlagParticipantNotFoundException` 발생

**FlagCommentQueryServiceTest**
- `getCommentCount()`: 댓글 없는 플래그 → 0 반환; 댓글 있는 플래그 → 정확한 카운트 반환 (Task 17 구현 검증)
- `getCommentTree()`: 비공개 댓글은 호스트/작성자 외 필터링됨; 재귀 트리 구조로 변환됨

**FlagCommentCommandServiceTest**
- `createRootComment()`: 정상 생성 성공; 플래그 미존재 시 `FlagNotFoundException` 발생
- `createReply()`: 부모 댓글 미존재 시 `FlagCommentNotFoundException` 발생; 2단 중첩 시 `FlagCommentReplyDepthException` 발생
- `updateComment()`: 작성자 수정 성공; 타인 수정 시 `FlagCommentAuthorizationException` 발생
- `deleteComment()`: 작성자 삭제 성공; 타인 삭제 시 예외 발생

**FlagMemorialCommandServiceTest**
- `createMemorial()`: 정상 생성 성공
- `updateMemorial()`: 작성자 수정 성공; 타인 수정 시 `FlagMemorialAuthorizationException` 발생
- `deleteMemorial()`: 작성자 삭제 성공; 미존재 시 `FlagMemorialNotFoundException` 발생

**FlagMemorialQueryServiceTest**
- `getMemorials()`: 플래그 추도문 목록 정상 반환; 플래그 미존재 시 예외 발생

### 3. 컨트롤러 슬라이스 테스트 (`BaseControllerTest` 상속, `@WithMockCustomUser`)

**FlagControllerTest**
- `POST /api/v1/flags`: 일반 생성 시 201 + `hostFlag()` 호출 검증; 앵코르 파라미터 포함 시 `encoreFlag()` 호출 검증
- `GET /api/v1/flags/friends`: 200 + `getFriendFlags()` 호출 검증
- `PATCH /api/v1/flags/{id}/details`: 200 + `modifyFlagDetails()` 호출 검증
- `PATCH /api/v1/flags/{id}/capacity`: 200 + `modifyFlagCapacity()` 호출 검증
- `PUT /api/v1/flags/{id}/schedule`: 200 + `reschedule()` 호출 검증
- `PATCH /api/v1/flags/{id}/schedule/deadline`: 200 + `closeRecruitment()` 호출 검증
- `DELETE /api/v1/flags/{id}`: 204 + `closeFlag()` 호출 검증
- `POST /api/v1/flags/{id}/participants`: 201 + `participateInFlag()` 호출 검증
- `DELETE /api/v1/flags/{id}/participants`: 204 + `leaveFlag()` 호출 검증

**FlagQueryControllerTest**
- `GET /api/v1/flags/me/hosting`: 200 + `getMyHostingFlags()` 호출 검증
- `GET /api/v1/flags/me/participating`: 200 + `getMyParticipatingFlags()` 호출 검증

**FlagCommentControllerTest**
- `GET /api/v1/flags/{flagId}/comments`: 200 + `getCommentTree()` 호출 검증
- `GET /api/v1/flags/{flagId}/comments/count`: 200 + `getCommentCount()` 호출 검증
- `POST /api/v1/flags/{flagId}/comments`: 201 + `createRootComment()` 호출 검증
- `POST /api/v1/comments/{parentId}/replies`: 201 + `createReply()` 호출 검증
- `PATCH /api/v1/comments/{commentId}`: 200 + `updateComment()` 호출 검증
- `DELETE /api/v1/comments/{commentId}`: 204 + `deleteComment()` 호출 검증

**FlagMemorialControllerTest**
- `POST /api/v1/flags/{flagId}/memorials`: 201 + `createMemorial()` 호출 검증
- `GET /api/v1/flags/{flagId}/memorials`: 200 + `getMemorials()` 호출 검증
- `PATCH /api/v1/flags/memorials/{id}`: 200 + `updateMemorial()` 호출 검증
- `DELETE /api/v1/flags/memorials/{id}`: 204 + `deleteMemorial()` 호출 검증

## Decisions

| 항목 | 결정값 | 비고 |
|------|--------|------|
| 작업 순서 | 도메인 → 서비스 → 컨트롤러 | 하위 계층부터 검증하여 상위 계층 신뢰도 확보 |
| 서비스 테스트 방식 | `@ExtendWith(MockitoExtension.class)` | TestContainers 불필요, 순수 비즈니스 로직만 검증 |
| 컨트롤러 테스트 방식 | `BaseControllerTest` 상속 | 프로젝트 규약(`TESTING-PROTOCOL.md`) 준수 |
| 인증 모킹 | `@WithMockCustomUser` | 모든 flag API는 인증 필요 |
| 비공개 댓글 가시성 | `FlagCommentTest` + `FlagCommentQueryServiceTest` 양쪽 검증 | 도메인 규칙 자체와 서비스 적용 모두 확인 |
| Repository 어댑터 테스트 | 제외 | JPA 쿼리 검증은 TestContainers 필요, 별도 task로 분리 가능 |

## API Contract Change
[ ] 없음
