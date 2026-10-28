# Task 17: Implement Comment Count & Refactor My Flags API

## Objective
하드코딩되어 있던 댓글 수 조회 기능(`0L` 반환)을 실제 DB 카운트 로직으로 구현하고, 불명확했던 '역할 기반 내 플래그 조회' 로직(특히 데드 코드였던 `GUEST` 분기)을 제거한다. 대신 프론트엔드의 명확한 관심사 분리를 위해 '내가 호스팅 중인 플래그'와 '내가 참여 중인 플래그' API를 두 개의 독립적인 엔드포인트로 분리한다.

## Domain Change
[ ] 없음

## Target Files
- `src/main/java/com/example/DunbarHorizon/flag/application/service/comment/FlagCommentQueryService.java` — `getCommentCount()` 구현
- `src/main/java/com/example/DunbarHorizon/flag/domain/comment/repository/FlagCommentRepository.java` — `countByFlagId` 추가 (필요 시)
- `src/main/java/com/example/DunbarHorizon/flag/application/port/in/FlagQueryUseCase.java` — 인터페이스 메서드 분리
- `src/main/java/com/example/DunbarHorizon/flag/application/service/flag/FlagQueryService.java` — 기존 `switch` 문 제거 및 메서드 분리
- `src/main/java/com/example/DunbarHorizon/flag/adapter/in/web/FlagQueryController.java` — API 엔드포인트 2개로 분할

## Requirements
1. **댓글 수 조회:** `FlagCommentQueryService.getCommentCount(Long flagId)`가 `FlagCommentRepository`를 통해 실제 댓글 개수를 카운트하여 반환하도록 구현한다.
2. **데드 코드 제거:** `FlagQueryService`의 `getMyFlagsByRole` 메서드와 `switch` 문, 그리고 `GUEST` 조건에 대한 처리를 완전히 삭제한다.
3. **서비스 분리:** 기존에 `private`으로 묶여있던 `getMyManagedFlags`와 `getParticipatingFlags`를 `public`으로 열어 `FlagQueryUseCase`에 선언한다.
4. **컨트롤러 분리:** `FlagQueryController`에서 기존 역할을 쿼리 파라미터로 받던 API를 제거하고, 호스팅 목록과 참여 목록을 조회하는 두 개의 명시적인 API 엔드포인트로 나눈다.

## Decisions
| 항목 | 결정값 | 비고 |
|------|--------|------|
| 댓글 카운트 쿼리 | `countByFlagId(Long flagId)` | Soft Delete 제외 여부는 기존 엔티티의 `@SQLRestriction` 적용을 따름 |
| 내 플래그 조회 구조 | API 2개로 완전히 분할 | 기존 Role Enum 파라미터 의존성 제거 |

## API Contract Change
[x] 있음

변경 전:
GET /api/v1/flags/me?role={HOST|PARTICIPANT|GUEST} — 단일 API에서 분기 처리

변경 후:
GET /api/v1/flags/me/hosting — 내가 호스트인 플래그 목록 반환
GET /api/v1/flags/me/participating — 내가 참여자 명단에 있는 플래그 목록 반환