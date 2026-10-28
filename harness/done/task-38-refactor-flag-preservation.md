# Task-38: Flag 보존 상태 관리 리팩토링

## 배경

`isPreserved` 상태를 갱신하는 책임이 이벤트 리스너 3곳에 분산되어 있다. 이벤트 유실 시 stale 상태로 인해 보존되어야 할 Flag가 soft delete 파이프라인에 진입할 위험이 있고, 보존 조건 변경 시 여러 파일을 수정해야 한다.

## 목표

- `FlagPreservationDomainService` 도입: 분산된 갱신 책임을 단일화
- `Flag.updateSoftDeleteProtection()` package-private 제한: 도메인 서비스만 접근 가능
- `FlagPreservationCriteria` 제거: 도메인 서비스가 직접 판단
- `isPreserved` → `softDeleteProtected` 리네이밍

## 핵심 결정사항

- `softDeleteProtected`는 만료 스케줄러의 soft delete 진입 여부를 제어한다. 물리 삭제 스케줄러는 이 값을 보지 않는다.
- 도메인 서비스는 `flag/domain/flag` 패키지에 위치하여 `Flag`의 package-private 메서드에 접근한다.
- 도메인 서비스는 도메인 레이어의 output port(repository interface)를 참조한다.
- 이벤트 리스너 3곳(`FlagMemorialEventListener`, `FlagDeletionEventListener`, `FlagEncoreEventListener`)은 `isPreserved` 직접 갱신 대신 `FlagPreservationDomainService.refresh(flagId)`를 호출하도록 교체한다.
- 이벤트 유실 시 `softDeleteProtected` stale 위험을 줄이기 위해 리스너에 `@TransactionalEventListener` 적용을 검토한다.

## 브랜치

`ai/refactor-flag-preservation`

## 결과

**상태:** 완료 (main 머지)

| 커밋 | 내용 |
|------|------|
| `1f7be91` | `FlagPreservationDomainService` 도입, `isPreserved` → `softDeleteProtected` 리네이밍, `FlagPreservationCriteria` 제거, 이벤트 리스너 3곳 단일화 |
| `f499c97` | `FlagPreservationDomainServiceTest` 단위 테스트 추가 (4케이스) |

**변경 파일:**
- `Flag.java` — `softDeleteProtected` 리네이밍, `updateSoftDeleteProtection()` package-private
- `FlagPreservationDomainService.java` — 신규 도메인 서비스
- `FlagPreservationCriteria.java` — 삭제
- `FlagMemorialEventListener.java` — `refresh()` 위임, `@TransactionalEventListener(BEFORE_COMMIT)` 적용
- `FlagDeletionEventListener.java` — `refresh()` 위임, `memorialRepository` 의존성 제거
