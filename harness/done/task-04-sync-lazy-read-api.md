**Description**
Outbox 이벤트 처리 지연이나 장애 발생 시, 사용자가 Social 프로필을 조회하는 시점에 Account 모듈의 최신 데이터를 가져와 동기화하는 안전망을 구축

**Prerequisites**
- Task 03 완료

**Technical Specification**
- **모듈 간 의존성 분리 (Hexagonal Architecture):** Social 모듈이 Account 모듈을 직접 참조하지 않고 Social의 adapter에서 Account의 UseCase 참조
- **Neo4j 제약 조건:** Social DB의 SocialUser 노드의 id에 unique 제약조건을 명시 
- **동시성 및 멱등성:** 스케줄러의 Outbox 처리와 Lazy Sync가 동시에 발생하여 `DataIntegrityViolationException`이 발생할 경우, 예외를 무시(Catch and Ignore)하고 정상 응답을 반환하도록 처리.

**Acceptance Criteria**
1. Neo4j DB에 `SocialUser` 노드의 `id` 속성에 대한 Unique 제약 조건이 적용되어 있어야 한다.
2. Social 프로필 조회 API 호출 시 Neo4j에 데이터가 없으면, Account 모듈의 데이터를 동기적으로 가져와야 한다.
3. 가져온 데이터를 기반으로 Neo4j에 `MERGE` 쿼리를 실행하여 노드를 생성하고 프로필을 반환해야 한다.
4. Account 모듈에도 해당 유저 데이터가 존재하지 않을 경우 404 Not Found 예외를 발생시켜야 한다.
5. 조회 시점의 데이터 갱신으로 인해 발생하는 DB 중복 제약 조건 위반 에러는 클라이언트에게 노출되지 않고 정상 처리되어야 한다.