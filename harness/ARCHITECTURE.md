# Backend Architecture & Coding Conventions

이 프로젝트는 Domain-Driven Design(DDD) 기반의 Hexagonal Architecture(Ports and Adapters)를 따르고, 유스케이스를 Command와 Query로 나눈다. 분리 기준은 §1의 service 항목에 있다.

## 1. 패키지 및 계층 분리 원칙 (Layered Architecture)
아래는 도메인 패키지(예: `account`, `social`, `flag`, `buzz`)의 책임별 기본 배치 기준이다. 각 도메인에는 필요한 패키지와 구성 요소만 둔다.

- **domain**: 핵심 비즈니스 로직, Entity, Value Object, Domain Event, 자체 Exception, Repository Interface(Port)가 위치한다.
- **application**:
    - `port/in`: UseCase 인터페이스 (Command와 Query를 분리).
    - `port/out`: 외부 시스템 호출이나 데이터 저장을 위한 인터페이스.
    - `service`: UseCase의 구현체. 도메인 객체를 지휘한다. 트랜잭션(`@Transactional`) 관리가 포함된다. 조회 전용 유스케이스는 `QueryService`로 분리한다. 명령 쪽도 따로 둘 만큼 커지면 `CommandService`를 짝으로 만든다.
    - `eventListener`: Spring `@EventListener` 또는 `@TransactionalEventListener`를 이용한 도메인 이벤트 처리기.
    - `dto`: UseCase 내부 계층에서 사용하는 DTO 또는 Result 객체.
- **adapter**:
    - `in/web`: REST Controller, API Request/Response DTO.
    - `in/scheduler`: Spring Scheduler (@Scheduled).
    - `out/persistence`: 데이터베이스 어댑터 구현체 및 Spring Data Repository. 저장소가 둘 이상이면 그 아래를 `jpa`, `neo4j`, `mongo`로 나눈다.
    - 그 밖의 out 어댑터는 역할 이름을 그대로 쓴다. 외부 서비스는 `email`, `jwt`, `encrypt`, `imageStorage`, `redis`, `infrastructure`처럼, 다른 도메인 호출은 그 도메인 이름으로 둔다(`buzz/adapter/out/social`, `social/adapter/out/account`, `flag/adapter/out/user`).

### 모델·도메인 서비스·애플리케이션의 책임 배치

- 모델이 자연스럽게 판단할 수 있는 생성·상태 변경 규칙은 모델에 둔다. 다른 모델의 상태가 필요하면 생성 문맥인 객체나 조회한 사실을 전달할 수 있다. `hasExistingEncore`는 사실이고 `canCreateEncore`는 허용 여부를 이미 결정한 값이므로 구분한다.
- 각 유스케이스의 트랜잭션, 정보 준비, 저장·삭제와 후속 호출 순서는 애플리케이션이 조율한다. 단일 유스케이스의 절차를 감싸기 위해 도메인 서비스를 추가할 필요는 없다.
- 여러 모델 사이의 정책 또는 여러 유스케이스가 공유해야 하는 도메인 연산은 도메인 서비스에 둘 수 있다. 클래스 수·규칙 수·조회 row 수만으로 도입 여부를 정하지 않는다.
- 공통 도메인 연산에 필요한 조회·잠금을 도메인 서비스가 소유할 수 있다. 잠금 사용만으로 해체하거나, 명명 일관성을 이유로 저장까지 일괄 이관하지 않는다. 호출자의 트랜잭션과 조회 순서 전제는 별도로 명시하고 테스트한다.

### Flag의 책임 배치

각 책임을 이 위치에 둔 이유는 [Task 131의 설계 결정](done/task-131-flag-domain-service-boundaries.md#decision)에 기록한다.

| 책임 | 담당 |
|------|------|
| 후기 생성 자격·내용 및 수정·삭제 권한 | `FlagMemorial`. 생성 시 Flag와 작성자의 참여 사실을 받는다. Flag에 후기 작성 정책을 추가하지 않는다 |
| 초대 생성 자격과 수락·삭제 권한 | `FlagInvitation`. Flag와 참여·중복 여부 등 생성 문맥을 받는다 |
| 앵콜 생성 자격과 Flag 수동 삭제의 상태 변경 | `Flag`. 앵콜 생성에는 기존 유효 앵콜의 존재 여부를 전달한다 |
| 직접 참여·초대 참여의 공통 잠금, 중복·인원 조회와 참여 판단 연결 | `FlagParticipationManager`. 참여자 저장·삭제는 호출자가 한다 |
| 후기·유효 앵콜 존재 조회와 자동 만료 면제 갱신 | `FlagExpiryExemptionUpdater`. 잠근 Flag를 받아 변경하며 저장은 호출자가 한다 |
| 후기·앵콜 생성·삭제 및 초대 처리의 유스케이스 순서 | 애플리케이션 서비스가 도메인 모델과 공통 도메인 서비스 호출을 조율한다 |

후기 생성·삭제와 앵콜 생성은 Flag 잠금 조회를 일반 조회보다 먼저 수행한다. 앵콜 수동 삭제는 대상 → 부모 잠금 순서를 유지한다. 원천 변경의 저장·삭제, 면제 재계산, Flag 저장을 같은 트랜잭션에서 수행한다. 초대 수락은 기존 READ_COMMITTED를 유지하고 공통 참여 경로를 호출한다.

## 2. 데이터베이스 사용 전략 (Polyglot Persistence)
기능의 특성에 따라 알맞은 저장소를 선택하여 사용한다.
- **JPA (MySQL)**: `account`, `flag`, `trace` 등 트랜잭션과 정형화된 관계형 데이터가 중요한 도메인.
- **Neo4j**: `social` 도메인. 친구 관계(Friendship), 친구 요청, 그래프 탐색(n-hop) 등 관계 중심 데이터.
- **MongoDB**: `buzz`, `notification` 등 수명이 생성 시점에 정해져 TTL로 관리되고, 수신자 명단 같은 값 목록을 도큐먼트 안에 함께 두는 도메인.

## 3. 통신 및 결합도 규칙
- **의존성 방향**: 소스 코드의 의존성을 기준으로, `domain`은 `application`과 `adapter`를 참조하지 않는다. `application`은 `adapter`의 구체 구현을 참조하지 않고 포트에 의존한다. `adapter`는 `application` 또는 `domain`에 정의된 포트를 구현할 수 있다.
- **애그리거트 간 정합성**: 여러 애그리거트의 변경이 함께 성공해야 한다면 같은 트랜잭션에서 명시적으로 조율한다. 하나의 트랜잭션으로 묶을 수 없는 저장소나 외부 시스템에 걸친 변경은 실패 복구와 정합성 보장 방식을 별도로 설계한다.
- **도메인 간 통신과 이벤트**: 다른 도메인의 기능은 공개된 포트를 통해 호출한다. 이벤트는 발생한 사실에 대한 후속 반응을 분리할 때 사용하며, 처리 시점과 실패 시 요구되는 정합성을 기준으로 선택한다. 이벤트 사용 자체가 비동기 실행이나 트랜잭션 분리를 의미하지는 않는다.

## 4. 보안 및 컨벤션
- 사용자 식별: Controller에서 현재 로그인한 사용자를 참조할 때는 커스텀 어노테이션 `@CurrentUserId`를 사용하라.
- Base Entity: 생성/수정 시간을 추적하는 JPA 엔티티는 `BaseTimeEntity` 또는 `BaseTimeAggregateRoot`를 상속한다. 아웃박스나 아카이브처럼 자체 시각 필드를 갖는 엔티티는 예외다. 논리적 삭제가 필요하면 `SoftDeletable`을 구현한다.

## 5. Restful API
- adapter/in/web의 controller의 API 주소는 Restful 하게 작성
