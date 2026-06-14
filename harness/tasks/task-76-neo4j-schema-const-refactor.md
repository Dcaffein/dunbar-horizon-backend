# Task-76: Neo4j 스키마 상수 일관성 리팩토링

## 배경 및 목적

Neo4j 어댑터 계층의 Cypher 쿼리와 도메인 엔티티 어노테이션에서 노드 레이블·관계 타입·프로퍼티 이름이 일부 하드코딩된 문자열로 남아 있다.
모든 Neo4j 식별자를 단일 상수로 관리해 Single Source of Truth를 확보한다.

## 의도

1. **`@Node` 어노테이션 상수화**: `Friendship`, `FriendRequest`, `SocialUser`, `Label` 엔티티의 `@Node("...")` 리터럴을 도메인 constants로 교체
2. **`SchemaInitializer` 삭제**: `FriendRequestSchemaInitializer`, `SocialUserSchemaInitializer` 제거 — DB 제약은 외부에서 직접 관리
3. **`SocialConnectionPathRepositoryAdapter` 쿼리 상수화**: 하드코딩된 레이블·관계·프로퍼티 이름을 constants로 교체

## Out of Scope

- `SocialGraphSchema` 구조 변경 (프로퍼티 상수는 어댑터에 유지)
- DB 제약 및 인덱스 관리
