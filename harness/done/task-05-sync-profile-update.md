**Description**
초기 생성뿐만 아니라 유저 정보(닉네임, 프로필 이미지 등) 변경 시에도 Outbox 패턴을 통해 Social 모듈로 데이터를 전파
현재 구현되어 있지 않은 controller의 api들도 구현해야 함
이때 네트워크 지연이나 재시도로 인해 이벤트 처리 순서가 역전되더라도 최종적인 데이터 정합성이 보장되어야 함

**Prerequisites**
- Task 03, Task 04 완료

**Technical Specification**
- 이벤트 페이로드를 추상화 하기 
- **순서 보장 (Last-Write-Wins):** Social 모듈은 이벤트를 수신하여 업데이트를 수행할 때, 기존 Neo4j 노드에 저장된 `updatedAt` 속성과 수신한 이벤트의 `occurredAt` 값을 비교.
    - 조건: `event.occurredAt > node.updatedAt` 인 경우에만 덮어쓰기(Update) 실행.
    - 이벤트의 타임스탬프가 더 과거인 경우, 이미 최신 데이터가 반영된 것으로 간주하고 업데이트 로직을 스킵(멱등성 보장).

**Acceptance Criteria**
1. Account 모듈에서 유저 정보 수정 로직 수행 시, 변경된 속성과 타임스탬프가 포함된 페이로드가 Outbox 테이블에 PENDING 상태로 저장되어야 한다.
2. Social 모듈은 업데이트 이벤트를 정상적으로 수신하고 Neo4j 노드의 속성을 갱신해야 한다.
3. 순서가 뒤바뀐 과거의 이벤트가 Social 모듈에 도달했을 때, Neo4j 노드의 데이터가 과거 데이터로 덮어씌워지지 않고 최신 상태를 유지해야 한다.
4. 업데이트 완료 시 Neo4j 노드의 `updatedAt` 속성이 이벤트의 `occurredAt` 값으로 갱신되어야 한다.