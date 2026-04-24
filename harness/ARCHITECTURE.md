# Backend Architecture & Coding Conventions

이 프로젝트는 Domain-Driven Design(DDD) 기반의 Hexagonal Architecture(Ports and Adapters)와 CQRS 패턴을 엄격하게 준수한다.

## 1. 패키지 및 계층 분리 원칙 (Layered Architecture)
모든 도메인 패키지(예: `account`, `social`, `flag`, `buzz`)는 아래의 내부 구조를 가져야 한다.

- **domain**: 핵심 비즈니스 로직, Entity, Value Object, Domain Event, 자체 Exception, Repository Interface(Port)가 위치한다. Spring 의존성을 최소화한 POJO로 작성한다.
- **application**:
    - `port/in`: UseCase 인터페이스 (Command와 Query를 분리).
    - `port/out`: 외부 시스템 호출이나 데이터 저장을 위한 인터페이스.
    - `service`: UseCase의 구현체. 도메인 객체를 지휘한다. 트랜잭션(`@Transactional`) 관리가 포함된다. 반드시 `CommandService`와 `QueryService` 클래스로 책임을 분리하라.
    - `eventHandler`: Spring `@EventListener` 또는 `@TransactionalEventListener`를 이용한 도메인 이벤트 처리기.
    - `dto`: UseCase 내부 계층에서 사용하는 DTO 또는 Result 객체.
- **adapter**:
    - `in/web`: REST Controller, API Request/Response DTO.
    - `in/scheduler`: Spring Scheduler (@Scheduled).
    - `out/persistence`: 데이터베이스(JPA, Neo4j, Mongo) 어댑터 구현체 및 Spring Data Repository.
    - `out/client`: 외부 API 연동 어댑터.

## 2. 데이터베이스 사용 전략 (Polyglot Persistence)
기능의 특성에 따라 알맞은 저장소를 선택하여 사용한다.
- **JPA (MariaDB)**: `account`, `flag`, `trace` 등 트랜잭션과 정형화된 관계형 데이터가 중요한 도메인.
- **Neo4j**: `social` 도메인. 친구 관계(Friendship), 친구 요청, 그래프 탐색(n-hop) 등 관계 중심 데이터.
- **MongoDB**: `buzz`(피드), `notification` 등 읽기/쓰기가 빈번하고 스키마 유연성이 필요한 도메인.

## 3. 통신 및 결합도 규칙
- **의존성 방향**: 모든 의존성은 어댑터(Adapter)에서 애플리케이션(Application)을 거쳐 도메인(Domain)을 향해야 한다. 반대 방향의 참조는 절대 금지한다.
- **도메인 간 통신**: 다른 도메인(Aggregates)의 상태를 변경해야 할 때는 직접 참조하지 말고 Spring ApplicationEvent(`DomainEvent`)를 발행하여 처리하라 (예: `UserInteractionEvent`, `NotificationEvent`).

## 4. 보안 및 컨벤션
- 사용자 식별: Controller에서 현재 로그인한 사용자를 참조할 때는 커스텀 어노테이션 `@CurrentUserId`를 사용하라.
- Base Entity: JPA 엔티티는 `BaseTimeEntity` 또는 `BaseTimeAggregateRoot`를 상속받아 생성/수정 시간을 자동 관리하라. 논리적 삭제가 필요하면 `SoftDeletable`을 구현하라.

## 5. Restful API
- adapter/in/web의 controller의 API 주소는 Restful 하게 작성