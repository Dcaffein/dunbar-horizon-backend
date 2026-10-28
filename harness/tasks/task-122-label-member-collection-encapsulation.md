# Task-122: Label의 멤버 컬렉션을 읽기 전용으로 닫는다

## Background

`Label`은 `Set<UserReference> members`를 `@Relationship(ATTACHED_TO)`으로 들고 있다. 그런데 멤버 추가와 교체는 `LabelMemberEnroller`가 `LabelRepository.addMember`, `replaceMembers` 같은 id 기반 명령으로 처리하고 이 컬렉션을 거치지 않는다. task-118에서 라벨 쓰기를 id 기반 Cypher로 옮긴 뒤로 쓰기 경로와 컬렉션은 완전히 분리됐다.

그런데 `Label`은 클래스 레벨 `@Getter`라 내부 `Set`이 그대로 노출된다. 호출부에서 `getMembers().add(...)`가 가능하다. 같은 도메인의 `Friendship`은 `getRecognitions()`에서 `List.copyOf`로 막아뒀으므로 두 기준이 공존하고 있다.

현재 `getMembers()` 호출부는 `LabelResult`의 `size()`와 `LabelService`의 `stream()`뿐이라 컬렉션을 변경하는 코드는 없다. 지금 깨지는 버그가 아니라, 쓰기 경로를 하나로 정리한 결정을 타입으로 고정하는 작업이다.

판정상 라벨 멤버는 Label의 자식 엔티티가 아니다. `ATTACHED_TO` 엣지에는 속성이 없고 `LabelMember` 같은 클래스도 없다. 들고 있는 것은 별개 애그리거트인 `UserReference`에 대한 참조이므로, Label이 소유하고 조작할 대상이 아니다. 속성을 가진 `FriendRecognition`이 `@RelationshipProperties`로 엔티티 지위를 얻은 것과 대비된다.

## Objective

Label 바깥에서 멤버 컬렉션을 바꿀 수 없게 만들어, 멤버 변경이 id 기반 명령 한 경로로만 일어나도록 고정한다.

## Decision

### `Friendship`의 기준에 맞춘다

`getRecognitions()`가 쓰는 방어적 복사와 같은 방식으로 반환한다. 같은 도메인 안에서 컬렉션 노출 기준이 둘로 갈리지 않게 한다.

### 컬렉션 매핑 자체는 유지한다

조회 경로에서 쓰이고, 관계를 따라가는 그래프 이점도 여기서 나온다. 매핑 제거는 대상이 아니다.

### 멤버를 조작하는 메서드를 Label에 추가하지 않는다

쓰기는 id 기반 명령이 맡는다는 task-118의 결정을 유지한다. 컬렉션을 닫는 것이 목적이지 Label에 쓰기 책임을 되돌리는 것이 아니다.

## Out of Scope

- 라벨 멤버 수 상한 정책
- 멤버 쓰기 경로 변경
- `UserReference` 모델과 동기화 방식
- `Label`의 `owner` 노출 방식

## Acceptance Criteria

- Label 바깥에서 반환받은 멤버 컬렉션을 변경해도 Label 내부 상태가 바뀌지 않는다.
- 기존 라벨 조회, 멤버 조회, 멤버 교체 테스트가 통과한다.
- `Friendship`과 `Label`이 컬렉션을 노출하는 방식이 같다.
