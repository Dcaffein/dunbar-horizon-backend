# Task-39: @SQLRestriction 관련 쿼리 버그 수정

## 배경

`@SQLRestriction("deleted_at IS NULL")`은 Hibernate가 해당 엔티티에 대한 SQL을 생성할 때 `AND deleted_at IS NULL`을 자동으로 삽입한다. soft delete된 레코드를 의도적으로 다뤄야 하는 코드들이 이를 고려하지 않아 두 곳에서 버그가 발생했다.

## 버그 목록

### 버그 1 — FlagDeletionEventListener 항상 예외 발생
`AFTER_COMMIT` 리스너에서 soft delete된 Flag를 `findById`로 재조회하면 `@SQLRestriction` 때문에 항상 `orElseThrow`가 발동한다. `@Async`라 원본 트랜잭션에 영향이 없어 눈에 띄지 않지만, 참여자 정리·알림·인터랙션 이벤트가 모두 무력화된 상태다.

수정 방향: `FlagDeletedEvent`에 `hostId`, `participantIds` 등 필요한 데이터를 담아 재조회를 제거한다.

### 버그 2 — 물리 삭제 스케줄러 항상 0건
`_findIdsInternal`이 JPQL이라 `@SQLRestriction`이 자동 적용된다. `deleted_at IS NULL AND deleted_at < ?` 조건이 되어 항상 0건이다.

수정 방향: `nativeQuery = true`로 전환한다.

### 버그 3 — FlagMemorialEventListener @Transactional 누락
`@EventListener`에 `@Transactional`이 없어 `flagRepository.save()` 호출 시 트랜잭션 컨텍스트가 불명확하다.

수정 방향: `@Transactional` 추가.

### 버그 4 — FlagEncoreEventListener occurredAt 누락
`NotificationEvent` 발행 시 `occurredAt`을 설정하지 않는다. 다른 리스너들은 모두 `LocalDateTime.now()`를 세팅한다.

수정 방향: `occurredAt(LocalDateTime.now())` 추가.

## 브랜치

`ai/fix-sqlrestriction-query-bugs`

## 결과

**상태:** 완료 (main 머지)

| 커밋 | 내용 |
|------|------|
| `267dff2` | 버그 1~4 일괄 수정 |

**버그별 수정 내역:**
- **버그 1** — `FlagDeletedEvent`에 `hostId` 추가, `FlagDeletionEventListener`에서 soft delete된 Flag 재조회 제거
- **버그 2** — `_findIdsInternal` → `nativeQuery = true`로 전환, `Pageable` 제거
- **버그 3** — `FlagMemorialEventListener.handleMemorialCreated/Deleted`에 `@Transactional` 추가 (task-38에서 `@TransactionalEventListener(BEFORE_COMMIT)`로 최종 적용)
- **버그 4** — `FlagEncoreEventListener` `NotificationEvent`에 `occurredAt(LocalDateTime.now())` 추가

**추가:** `FlagDeletionEventListenerTest` 단위 테스트 3케이스 추가, `FlagTest`에 `delete_PublishesEventWithHostId` 테스트 추가
