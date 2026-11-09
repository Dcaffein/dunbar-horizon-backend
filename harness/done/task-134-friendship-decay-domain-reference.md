# Task-134: 감쇄 규칙을 도메인 메서드로 두고 쿼리와 같은 결과인지 검증한다

## Background

관심도 감쇄는 `FriendshipDecayService`가 `lockDecayTargets`로 대상을 잠근 뒤 `FriendshipNeo4jRepository.applyDecay` Cypher로 처리한다(task-130). 마지막 교류 뒤 30일이 지난 쪽의 관심도에 감쇄율을 곱하되 하한 아래로는 내리지 않고, 양쪽 관심도로 친밀도를 다시 계산한다. 감쇄율, 하한, 기준 시각은 `FriendshipDecayPolicy`가 정한다.

이 규칙은 지금 Cypher 안에만 있다. 친밀도 공식(`score / (score + 50)`의 곱의 제곱근)도 쿼리에 숫자로 다시 적혀 있다. 도메인 쪽에는 `Friendship.recalculateIntimacy`와 `FriendRecognition.CONVERGENCE_K`가 있지만, 감쇄를 표현하는 메서드는 없다.

관심도·친밀도의 단건·모임 갱신은 도메인이 계산한다(task-130). 감쇄만 대상이 관계 전체(실험 데이터 기준 약 107만 개)라 애플리케이션으로 불러오는 비용이 커서 Cypher에 남겼다. 그 대가로 감쇄 규칙이 쿼리로 새어 나간 것을, 포트폴리오 사례 3은 "도메인 메서드로 수행한 결과와 쿼리로 수행한 결과가 같은지 테스트로 확인하는 것으로 타협했다"고 적었다. 지금 코드에는 그 도메인 메서드와 테스트가 없어서 문서가 사실과 다르다.

## Objective

`Friendship`에 감쇄 규칙을 표현하는 도메인 메서드가 생기고, 같은 입력에서 `applyDecay` 쿼리와 도메인 메서드가 같은 관심도와 친밀도를 내는지 테스트가 확인한다. 감쇄 규칙의 기준은 도메인에 있고, 쿼리는 그 규칙을 대량으로 실행하는 구현이 된다.

## Decision

### 실제 감쇄는 계속 Cypher가 한다

운영 경로는 바꾸지 않는다. 도메인 메서드는 규칙의 기준이자 검증용이다. 관계 전체를 애플리케이션으로 불러와 도메인으로 계산하는 방식은 비용 때문에 택하지 않는다.

### 도메인 메서드는 기존 도메인 규칙을 재사용한다

친밀도 재계산은 `recalculateIntimacy`를, 정규화 상수는 `FriendRecognition`을 쓴다. 감쇄율·하한·기준 시각은 `FriendshipDecayPolicy`의 값을 인자로 받는다. 같은 규칙을 도메인 안에 두 번 쓰지 않는다.

### 비교 테스트가 다룰 경우

- 양쪽 모두 감쇄 대상
- 한쪽만 감쇄 대상(다른 쪽은 최근 교류)
- 감쇄하면 하한 아래로 떨어지는 경우
- 이미 하한 이하인 경우
- 양쪽 모두 감쇄 대상이 아닌 경우

부동소수 비교는 허용 오차를 두고, 오차 값은 PLAN에서 정한다.

## Out of Scope

- 감쇄를 도메인 계산으로 옮기는 운영 경로 변경
- 감쇄율, 유예 기간, 하한 같은 정책 값 변경
- `lockDecayTargets`의 잠금 방식

## Acceptance Criteria

- `Friendship`에 감쇄 도메인 메서드가 있고, 친밀도 재계산은 기존 도메인 메서드를 거친다.
- 위 다섯 경우에서 `applyDecay`를 실행한 결과와 같은 입력에 도메인 메서드를 적용한 결과가 허용 오차 안에서 같다는 테스트가 통과한다.
- 쿼리의 감쇄식이나 친밀도 공식을 일부러 바꾸면 이 테스트가 실패하는 것을 확인한다.
- 기존 감쇄 테스트(`FriendshipNeo4jRepositoryTest`, `FriendshipDecayServiceTest`, `FriendshipDecayPolicyTest`)가 통과한다.
