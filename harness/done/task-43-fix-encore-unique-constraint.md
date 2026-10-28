# Task-43: Encore 중복 생성 race condition 수정

## 배경

`FlagEncoreCreator.encore()`에서 `existsByParentId` → `createEncore` 순서로 check-then-act 패턴을 사용한다. DB에 `parent_id`에 unique constraint가 없어 동시 요청 시 중복 encore가 삽입될 수 있다.

## 목표

DB 레벨 unique constraint로 중복 encore 삽입을 원천 차단한다.

## 핵심 결정사항

- `flags` 테이블 `parent_id` 컬럼에 unique constraint 추가 (`NULL` 허용 — 일반 Flag는 parentId가 없음)
- `@Table(uniqueConstraints = ...)` 또는 `@Column(unique = true)`로 JPA 레벨에도 선언
- `DataIntegrityViolationException` 발생 시 `FlagInvalidStatusException`으로 변환하는 예외 처리 추가

## 브랜치

`ai/fix-encore-unique-constraint`

## 결과

**상태:** 완료 (main 머지)

| 커밋 | 내용 |
|------|------|
| `397eec2` | `Flag.java` unique constraint 추가, `FlagHostService` 예외 변환, `FlagHostServiceTest` 케이스 추가 |

**변경 내역:**
- `Flag.java` — `@Table`에 `uq_flags_parent_id` unique constraint 선언 (`parent_id`, NULL 허용)
- `FlagHostService.encoreFlag()` — `flagRepository.save()` 호출부를 try-catch로 감싸 `DataIntegrityViolationException` → `FlagInvalidStatusException` 변환
- `FlagHostServiceTest` — 동시 요청 시 unique 제약 위반 케이스 추가
