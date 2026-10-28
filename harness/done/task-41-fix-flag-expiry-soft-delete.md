# Task-41: 만료 스케줄러 soft delete 수정

## 배경

`expireAllExceedingThreshold`가 JPQL bulk DELETE로 작성되어 있어 `@SQLDelete` 훅을 무시하고 즉시 물리 삭제를 수행한다. "만료 마킹"이라는 의도와 실제 동작이 다르다.

또한 보존 조건으로 memorial만 직접 조회하고 있어, encore를 고려하지 않고 태스크-38에서 도입하는 `softDeleteProtected` 상태를 무시한다.

## 목표

- JPQL DELETE → `UPDATE ... SET deleted_at = :now` 로 전환해 soft delete로 교정
- memorial 직접 조회 조건 제거, `softDeleteProtected = false` 조건으로 교체

## 의존 관계

태스크-38(`softDeleteProtected` 도입) 완료 이후 적용한다.

## 브랜치

`ai/fix-flag-expiry-soft-delete`

## 결과

**상태:** 완료 (main 머지)

| 커밋 | 내용 |
|------|------|
| `99653ee` | `expireAllExceedingThreshold` JPQL DELETE → UPDATE 전환, `FlagExpiryServiceTest` 추가 |

**변경 내역:**
- `FlagJpaRepository.expireAllExceedingThreshold` — `DELETE` → `UPDATE Flag f SET f.deletedAt = CURRENT_TIMESTAMP`로 교체, 조건을 `softDeleteProtected = false AND deletedAt IS NULL`로 교체 (memorial 직접 조회 제거)
- 시그니처 불변 — `FlagRepository` 포트, `FlagExpiryService` 호출부 변경 없음
- `FlagExpiryServiceTest` 신규 추가 (임계값 계산 검증, 레포지터리 호출 검증)
