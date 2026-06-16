# Task-84: Neo4j 스키마 제약 조건 관리 체계 수립

## 배경

Task-83 작업 중 `Friendship.id`에 인덱스가 없어 직접 접근 쿼리들이 풀스캔을 발생시킨다는 점이 확인됐다.
Social 도메인 전반에 걸쳐 Neo4j 노드 프로퍼티에 대한 constraint/index 현황을 점검하고,
RDB 도메인들의 JPA 컨벤션에 대응하는 Neo4j 스키마 관리 방식을 정립한다.

---

## 검토 범위

### Neo4j 노드 타입 및 식별자 현황

| 노드 | 식별자 필드 | 현재 index/constraint 여부 |
|------|------------|--------------------------|
| `UserReference` (`USER_REFERENCE`) | `id: Long` | 확인 필요 |
| `Friendship` (`FRIENDSHIP`) | `id: String` (composite) | 없음 (확인됨) |
| `Label` | 확인 필요 | 확인 필요 |

### 확인 사항

1. **현재 Neo4j 스키마 현황** — `SHOW INDEXES`, `SHOW CONSTRAINTS`로 실제 걸려있는 것 확인
2. **각 노드별 필요 constraint** — `@Id` 필드 기준 unique constraint 누락 여부
3. **관계 프로퍼티 인덱스** — 쿼리 조건으로 자주 쓰이는 `r.isMuted`, `r.lastInteractedAt` 등 필요 여부

---

## 배경 지식: SDN6와 Neo4j 스키마 관리

SDN6에는 JPA의 `@Column(unique = true)` / `hbm2ddl.auto`에 해당하는 필드 레벨 어노테이션이 없다.
constraint는 반드시 Cypher로 직접 실행해야 한다.

### 선택 가능한 관리 방식

| 방식 | 특징 |
|------|------|
| `@PostConstruct`에서 `Neo4jClient`로 실행 | 앱 기동 시 자동 적용, 별도 도구 불필요, `IF NOT EXISTS`로 멱등 보장 |
| `neo4j-migrations` 라이브러리 | RDB의 Flyway/Liquibase 역할, 마이그레이션 이력 관리 가능 |
| 수동 실행 | 일회성, 자동화 안됨 |

현재 프로젝트에 Neo4j 스키마 관리 인프라가 없으므로, 이 작업에서 컨벤션을 정하고 일괄 적용한다.

---

## 브랜치

`ai/feat-neo4j-schema-constraint`
