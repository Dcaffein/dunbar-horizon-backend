# Task-118: Friendship·Label 쓰기 경로의 SDN save 제거 및 Cypher 전환

## Background

SDN `save()`는 저장 대상 노드만 쓰지 않는다. 관계로 도달 가능한 노드를 같은 쓰기 단위에 포함해 함께 쓴다.

`Friendship.recognitions[].user`와 `Label.owner`·`Label.members`는 `UserReference`를 ID가 아니라 객체로 들고 있다. `@TargetNode`의 선언 타입이 인터페이스지만 SDN은 구현체 `SocialUser`로 매핑한다. 따라서 `Friendship` 하나를 저장하면 `SocialUser` 노드도 함께 쓰인다.

`SocialUser`는 account 도메인에서 outbox를 거쳐 동기화되는 별개 애그리거트다. 쓰기의 정당한 주체는 `SocialUserSyncCommandService` 하나뿐이다. 그런데 위 구조 때문에 `Friendship`과 `Label`이 의도 없이 같은 노드의 writer가 된다.

### 확인된 증상

Neo4j testcontainer 프로브로 두 가지를 재현했다.

**라벨 부활.** `SocialUser.statusLabels`는 `@DynamicLabels`다. SDN은 저장 시점에 DB의 현재 라벨을 읽어 diff를 내지만, 메모리 스냅샷을 정답으로 삼는다. 그래서 `REMOVE :InactiveSocialUser` + `SET :UserReference`가 생성된다.

```text
저장 전 B 라벨      = [SocialUser, UserReference]
비활성화 후 B 라벨   = [SocialUser, InactiveSocialUser]
save() 후 B 라벨     = [SocialUser, UserReference]
```

`(:UserReference)` 라벨 매칭이 네트워크 조회, 친구 목록, 라벨 멤버, 친구 요청의 공통 필터이므로 비활성 사용자가 타인의 그래프에 다시 노출된다.

**속성 되돌림.** 캐스케이드는 노드 속성 집합 전체를 교체한다. 변경 필드만 고르는 더티 체킹이 아니다.

```text
스냅샷 B           = 사용자B / oldImage / updatedAt=null
갱신 후 DB B        = 새닉네임 / newImage / updatedAt=2026-10-14T14:55:05
캐스케이드 save 후 B = 사용자B / oldImage / updatedAt=null
```

`updatedAt`이 `null`로 되돌아가면 `SocialUser.isNewerThan`이 무조건 true를 반환한다. 순서 역전을 막기 위한 Last-Write-Wins 가드가 해제되고, 이후 도착하는 첫 `PROFILE_UPDATE`는 `occurredAt`이 얼마나 오래된 이벤트든 적용된다. outbox 재시도로 묵은 이벤트가 늦게 들어오는 구조에서 정확히 그 장치가 무력화된다.

### 레이스 윈도우와 복구 불가

같은 트랜잭션에서 load 직후 save면 스냅샷이 최신이라 쓰기가 멱등하다. 문제는 load와 save 사이에 `DEACTIVATE` 또는 `PROFILE_UPDATE`가 커밋되는 경우다.

그 이벤트는 `SocialUserEventListener`의 `@Async` + AFTER_COMMIT으로 들어오고 outbox 재시도 스케줄러도 끼어 있다. 사용자 요청과 동기화되어 있지 않으므로 임의 시점에 임의 사용자의 노드가 바뀐다. 윈도우가 좁다고 가정할 수 없다.

outbox는 상태 기반 재시도만 수행하고 재조정 잡이 없다. 한 번 어긋나면 다음 `ACTIVATE`/`DEACTIVATE`가 올 때까지 영구히 유지된다.

### 이미 진행된 전환

이 전환은 새로 도입하는 패턴이 아니라 끝나지 않은 작업이다.

`FriendRequest`도 `UserReference` 두 개를 객체로 들고 있는 같은 모양이지만, `FriendRequestRepositoryAdapter`는 `save()`를 쓰지 않는다. 생성은 `mergeFriendRequest`, 상태 변경은 `updateFriendRequest`로 모두 커스텀 Cypher다.

`Friendship`도 절반 이상 전환되어 있다. `updateUserFields`, `incrementInterestScore`, `incrementMutualInterestScore`, `incrementMutualInterestScoresBatch`, `applyDecay`가 전부 커스텀 Cypher다. `save()`는 친구 요청 수락 경로 하나에만 남은 잔존물이다. `Label`은 아직 전부 `save()`다.

### 멤버 수정의 현재 구현과 단건 제거 결함

라벨 멤버를 바꾸는 세 연산은 모두 집합 전체를 읽어 메모리에서 바꾼 뒤 통째로 다시 쓴다.

| 연산 | 메모리 변경 | 영속화 |
|------|-----------|-------|
| `addMemberToLabel` | `members.add(새로 조회한 인스턴스)` | `save` → 집합 전체 교체 |
| `removeMemberFromLabel` | `members.remove(별도 조회한 인스턴스)` | `save` → 집합 전체 교체 |
| `replaceLabelMembers` | `members.clear()` + `addAll()` | `save` → 집합 전체 교체 |

add와 remove가 논리적으로는 델타지만 영속화 단계에서는 셋 다 절대 교체다.

그리고 `removeMemberFromLabel`은 동작하지 않는다. `SocialUser`에 `equals`/`hashCode`가 없어 `HashSet.remove`가 동일성 비교를 수행한다. 그런데 `LabelService`는 `socialUserRepository.findById`로 따로 조회한 인스턴스를 넘기고, 집합 안의 인스턴스는 `labelRepository.findById`가 매핑할 때 만든 다른 객체다.

```text
같은 인스턴스인가 = false
equals 결과      = false
remove 후 메모리 멤버 수 = 1
save 후 DB 관계 수       = 1
```

그래서 `DELETE /api/v1/labels/{labelId}/members/{memberId}`는 아무 일도 하지 않고 204를 반환한다.

### 친구 끊기 시 라벨이 고아가 되는 결함

`LabelMemberShipEventListener`는 집합에서 꺼낸 인스턴스를 넘기므로 동일성 비교 결함은 피한다. 그러나 라벨을 읽는 `findLabelsByOwnerAndMember`가 `RETURN label`만 반환해서, SDN은 `owner = null`, `members = []`인 객체를 만든다. 리스너는 빈 집합에서 대상을 못 찾고, 이어지는 `saveAll`이 빈 상태를 정답으로 써서 `OWNS_LABEL`과 모든 `ATTACHED_TO`를 지운다.

```text
before        = {owners=1, members=[2, 3]}
loaded owner  = null, loaded members = 0
after saveAll = {owners=0, members=[]}
```

친구 하나를 끊으면 그 친구가 들어 있던 라벨이 소유자의 라벨 목록에서 사라진다.

두 결함 모두 기존 테스트가 잡지 못했다. 멤버 제거 검증이 전부 목 기반이고, 저장소 테스트는 라벨 이름만 단언하며, Neo4j에서 관계가 남았는지 사라졌는지 확인하는 테스트가 없었다.

### 라벨 이름 변경의 소유자 검증 누락

`LabelService.updateLabel`은 `currentUserId`를 받지만 쓰지 않는다. labelId만 알면 남의 라벨 이름을 바꿀 수 있다.

### 낙관적 락의 현재 실효성

`Label.version`은 API 응답과 요청 DTO 어디에도 노출되지 않는다. 클라이언트가 version을 받지 않으므로 되돌려줄 수 없다.

`replaceLabelMembers`에서 사용자가 보호받아야 하는 구간은 `GET /labels/{id}/members`로 목록을 보고 UI에서 선택해 `PUT`으로 보내는 사이다. 그 구간은 HTTP 왕복 바깥이므로 DB 트랜잭션으로 덮을 수 없다. 현재 `@Version`이 실제로 잡는 것은 동시에 날아온 PUT 두 개뿐이다.

`addMemberToLabel`과 `removeMemberFromLabel`은 성격이 다르다. 서로 다른 관계를 건드리는 델타 연산이므로 덮어쓸 대상이 없다. 그런데 현재는 집합 전체를 교체하는 `save()`로 구현되어 있어, 동시에 멤버를 둘 추가하면 하나가 `OptimisticLockingFailureException`으로 실패한다. 락이 보호가 아니라 비용으로만 작용한다.

## Objective

`Friendship`과 `Label`의 쓰기에서 `SocialUser` 노드가 함께 쓰이는 경로를 제거한다.

- 도메인 포트에서 `save`·`saveAll`과 엔티티를 인자로 받는 `delete`를 제거해, 애플리케이션 계층이 캐스케이드 쓰기를 호출할 방법을 없앤다.
- 두 애그리거트의 모든 쓰기를 ID 기반 Cypher로 수행한다.
- 멤버 추가·삭제를 집합 교체에서 델타 연산으로 바꾸고 낙관적 락을 제거한다.

다음 도메인 결과는 유지한다.

- 라벨 멤버는 라벨 소유자와 친구인 사용자만 될 수 있다.
- 라벨 수정은 소유자만 할 수 있다. 이름 변경은 지금 이 규칙을 지키지 않으므로 함께 고친다.
- 비활성 사용자는 소셜 그래프 어디에서도 보이지 않는다. 비활성화는 `:UserReference` 라벨을 떼는 것으로 표현하며, 다시 붙이면 라벨 멤버십을 포함해 원래 상태로 보여야 한다.
- 친구 요청 수락은 `Friendship` 노드와 양방향 `HAS_FRIENDSHIP` 관계를 만들고, 관계 속성 초기값과 `intimacy`는 현재 도메인 계산과 동일하다.
- 친구 관계 해제 시 양쪽 라벨에서 상대가 제거된다.

## Domain Change

[ ] 없음  [x] 있음

- `Label` 멤버 변경의 의미가 집합 교체에서 델타로 바뀐다. 동시 멤버 추가 두 건이 모두 반영되며, 기존처럼 하나가 충돌로 실패하지 않는다.
- `replaceLabelMembers`는 last-write-wins가 된다. 동시 PUT 두 건 중 나중 것이 이기고 에러가 발생하지 않는다.
- `LabelMemberEnroller`가 로드된 `Label` 엔티티 대신 식별자를 받는다. 친구 여부 검증과 중복 검증이 조회 쿼리로 수행된다.
- `Label.removeMember`가 `UserReference` 객체 대신 식별자를 받거나 제거된다. 객체를 받는 시그니처 자체가 동일성 비교 결함의 원인이다.
- 단건 멤버 제거가 실제로 반영된다. 기존에는 응답만 204였고 그래프는 변하지 않았다.
- 쓰기 경로에서 `UserReference` 객체 적재가 사라진다. 읽기 경로의 매핑과 조회 결과는 바뀌지 않는다.
- 친구 끊기 후 라벨의 소유 관계와 다른 멤버가 남는다. 기존에는 라벨 전체가 고아가 됐다.
- `PATCH /api/v1/labels/{labelId}`가 비소유자 요청에 403(`LABEL_ACCESS_DENIED`)을 반환한다.
- 비활성 친구를 멤버로 추가하면 404(`SOCIAL_USER_NOT_FOUND`)를 반환한다. 기존에는 비활성 사용자가 멤버로 들어갔다.
- 친구 요청 수락 시 한쪽이 비활성이면 404(`SOCIAL_USER_NOT_FOUND`)로 실패하고 요청이 남는다. 기존에는 비활성 사용자와도 친구 관계가 만들어졌다.
- 멤버 교체는 보이는 멤버만 비교한다. 비활성 멤버의 관계는 남아 재활성화 시 라벨에 다시 보인다. 친구 끊기 정리는 비활성 여부와 관계없이 관계를 지운다.

## Decision

### 대상은 `FriendshipRepository`와 `LabelRepository`로 한정한다

`SocialUserRepository.save`와 `saveAll`은 유지한다. 그 경로에서 `SocialUser`는 저장 대상 본인이고, `SocialUserSyncCommandService`는 이 노드의 유일한 정당한 writer다. 제거하면 비활성화와 프로필 동기화를 적용할 수단이 없어진다.

같은 이유로 `SocialUser`의 `@DynamicLabels`도 유지한다. 캐스케이드 경로가 닫히면 라벨을 쓰는 주체가 동기화 서비스 하나로 수렴하므로, 라벨 모델 자체를 바꿀 필요가 없다.

### 포트에서 메서드를 제거해 컴파일 단계에서 막는다

"`save()`를 쓰면 안 된다"는 제약을 규율에 맡기지 않는다. 현재 포트 시그니처에는 그 위험이 드러나지 않아, 다음 기능 추가에서 같은 사고가 재현된다.

`FriendshipRepository.save(Friendship)`, `LabelRepository.save(Label)`, `LabelRepository.saveAll(List<Label>)`, `LabelRepository.delete(Label)`을 제거하고 의도가 드러나는 메서드로 대체한다. `Neo4jRepository.save` 호출은 어댑터 내부에도 남기지 않는다.

### 낙관적 락을 완전히 제거한다

`Label`의 `@Version` 필드를 제거한다.

- `addMemberToLabel`·`removeMemberFromLabel`은 델타이므로 락이 보호할 대상이 없고, 현재는 정상 동작을 충돌로 거절하는 역할만 한다.
- `updateLabel`은 단일 속성 갱신이므로 락의 유무가 결과를 바꾸지 않는다.
- `replaceLabelMembers`만 절대 교체이지만, version이 클라이언트에 노출되지 않아 현재도 `GET`과 `PUT` 사이를 보호하지 못한다. If-Match 없는 PUT은 HTTP 의미상 last-write-wins이며, 라벨은 소유자 본인만 수정하므로 같은 사용자가 두 탭에서 동시에 저장하는 경우에 한정된다. 이 손실을 수용한다.

version을 API에 노출해 실제 락을 구는 안은 DTO·컨트롤러 변경과 프론트엔드 협의가 필요하므로 이번 작업에서 다루지 않는다.

### `SocialUser`에 `equals`/`hashCode`를 추가하지 않는다

단건 제거 결함의 직접적인 원인은 동일성 비교지만, 동치성을 추가해 고치지 않는다. 전환 후에는 집합 변경으로 쓰기를 수행하지 않으므로 그 동치성이 쓰이는 곳이 없어진다.

고칠 대상은 비교 방식이 아니라 "객체를 모아 집합을 만들고 통째로 저장한다"는 구조다. 멤버 제거를 `MATCH ... DELETE r`로 바꾸면 식별자로만 매칭하므로 결함이 성립하지 않는다.

### 읽기 매핑은 그대로 둔다

`@Relationship` 매핑은 읽기 경로만 만든다. 위험은 매핑이 아니라 `save()` 호출에서 발생한다. `findById`, `findByUserId`, `findAllByOwner_Id` 등의 조회와 반환 구조는 바꾸지 않는다. 조회 편의를 포기하지 않고 쓰기 수단만 교체한다.

### 관계 속성 초기값은 도메인이 계산한다

생성 Cypher에 `INITIAL_RAW_SCORE`나 정규화 상수를 직접 박지 않는다. `FriendRecognition`과 `Friendship`의 계산 메서드로 값을 구해 파라미터로 전달한다. 점수 정책의 소유권은 도메인에 남긴다.

### 멤버 교체는 DB 안에서 델타를 계산하는 한 문장으로 처리한다

전체 삭제 후 재생성은 바뀌지 않은 멤버의 관계까지 지우고 다시 만든다. 애플리케이션에서 델타를 계산하면 현재 멤버를 읽는 시점과 쓰는 시점 사이에 틈이 생긴다. 한 문장 안에서 기존 관계와 비교하면 둘 다 피한다.

락 없이는 동시 교체 두 건이 서로가 만든 관계를 못 본 채 각자 지우고 만들어 두 목록의 합집합이 남는다. 라벨 노드 쓰기 락을 먼저 잡아 뒤 트랜잭션이 앞 트랜잭션의 커밋 결과를 보고 비교하게 한다.

### 멤버 매칭은 `:UserReference`, 친구 끊기 정리만 `:SocialUser`

`:UserReference`는 개별 쿼리의 조건이 아니라 소셜 그래프 안의 존재 여부다. 멤버 조회·추가·제거·교체가 `:SocialUser`로 매칭하면 소유자가 볼 수 없는 비활성 멤버를 교체 시 "목록에 없음"으로 판정해 지우고, 재활성화해도 돌아오지 않는다.

친구 끊기 정리는 친구 관계가 실제로 끝난 것이므로 비활성 여부와 관계없이 지운다. 거르면 친구가 아닌 사람이 재활성화 후 라벨에 다시 나타난다.

### `DuplicateLabelMemberException`은 유지한다

존재 조회 후 `MERGE`한다. 409 응답 계약이 그대로 남는다. 조회와 쓰기 사이에 같은 멤버 추가가 동시에 들어오면 `MERGE`가 관계를 하나만 남기고, 한쪽이 409 대신 201을 받을 뿐이다.

### `createLabel`은 메모리 객체를 그대로 반환한다

생성 직후 라벨은 멤버가 없어 메모리 객체와 DB 상태가 같고, 응답에 필요한 값이 모두 있다. 재조회하지 않는다.

### 노드·관계 속성은 SDN 매핑에서 꺼낸다

생성 Cypher에 속성 이름을 나열하지 않는다. SDN 매핑 컨텍스트의 binder로 엔티티 자신의 속성만 Map으로 꺼내 `SET n += $props`로 넣는다. 필드가 늘어도 Cypher를 고칠 필요가 없고, 관계와 연결 노드는 binder 결과에 포함되지 않아 캐스케이드가 생기지 않는다.

### 기존 `version` 속성은 배포 후 수동으로 지운다

```cypher
MATCH (l:Label) WHERE l.version IS NOT NULL REMOVE l.version
```

코드에서 락이 사라졌는데 DB에 값이 남으면 락이 있는 것처럼 보인다. 반드시 새 코드 배포 후에 실행한다. 구 코드는 `version`이 null인 `Label`을 새 엔티티로 보고 `save` 시 새 노드를 만든다.

## Scope

1. `FriendshipRepository`에서 `save`를 제거하고, 친구 요청 수락용 생성 Cypher를 추가한다. `Friendship` 노드와 양방향 `HAS_FRIENDSHIP` 관계를 관계 속성까지 한 문장으로 만든다.
2. `LabelRepository`에서 `save`·`saveAll`·`delete(Label)`을 제거하고, 라벨 생성·이름 변경·멤버 추가·멤버 삭제·멤버 교체·삭제를 각각 ID 기반 Cypher로 구현한다.
3. `Label`에서 `@Version version` 필드를 제거한다.
4. `LabelMemberEnroller`가 로드된 엔티티 대신 식별자를 받도록 바꾸고, 중복 멤버 검증을 조회 쿼리로 전환한다. `filterFriendIdsAmong` 기반 친구 검증은 유지한다.
5. `LabelMemberEnroller.updateMembers`에서 `findAllUserReferencesById` 적재를 제거한다. 쓰기가 ID 기반이 되어 불필요하다.
6. `LabelMemberShipEventListener`의 라벨 전체 로드 후 `saveAll`을 단일 Cypher 관계 삭제로 교체한다.
7. 캐스케이드 회귀 테스트를 추가한다. 비활성 사용자가 포함된 그래프에 쓰기를 수행한 뒤 `InactiveSocialUser` 라벨이 유지되고, 다른 트랜잭션이 갱신한 `nickname`·`profileImageUrl`·`updatedAt`이 되돌아가지 않음을 검증한다.
8. 동시 멤버 추가 두 건이 모두 반영되는 테스트를 추가한다.
9. 단건 멤버 제거가 그래프에 반영되는지 검증하는 테스트를 추가한다. 목으로 호출 여부만 확인하지 않고 `ATTACHED_TO` 관계가 사라졌는지 확인한다.
10. `LabelService.updateLabel`에 소유자 검증을 추가한다.

## Out of Scope

- `SocialUserRepository`의 `save`·`saveAll` 제거
- `SocialUser`의 `@DynamicLabels` 제거 및 상태를 라벨에서 속성으로 옮기는 변경
- `(:UserReference)` 라벨 기반 필터링을 사용하는 기존 Cypher의 수정
- `FriendRequest` 영속화 경로 변경. 이미 커스텀 Cypher다
- `version`을 API에 노출하는 낙관적 락 재도입 및 그에 따른 DTO·컨트롤러 변경
- 관심도·친밀도 정책, 정규화 상수, decay 정책의 변경
- 네트워크 조회 캐시 및 그 무효화 정책
- account 도메인의 outbox 전달 방식과 재조정 잡 추가
- 이미 어긋난 운영 데이터의 보정

## Acceptance Criteria

- `FriendshipRepository`와 `LabelRepository`에 `save`·`saveAll`과 엔티티를 인자로 받는 `delete`가 존재하지 않는다.
- `Friendship`과 `Label`의 어댑터가 `Neo4jRepository.save`를 호출하지 않는다.
- `Label`에 `@Version` 필드가 없고, 라벨 수정 경로에서 `OptimisticLockingFailureException`이 발생하지 않는다.
- 비활성 사용자가 구성원인 `Friendship`을 생성하거나 라벨을 수정한 뒤에도 해당 사용자의 라벨이 `InactiveSocialUser`로 유지된다.
- 다른 트랜잭션이 `SocialUser`의 프로필을 갱신한 직후 `Friendship` 또는 `Label` 쓰기를 수행해도 `nickname`·`profileImageUrl`·`updatedAt`이 변하지 않는다.
- 같은 라벨에 서로 다른 멤버를 동시에 추가하면 두 건 모두 반영된다.
- 단건 멤버 제거 후 해당 `ATTACHED_TO` 관계가 그래프에서 사라진다.
- 친구 요청 수락 후 생성된 `Friendship`의 관계 속성과 `intimacy`가 기존 도메인 계산 결과와 동일하다.
- 친구 관계 해제 후 양쪽 소유 라벨에서 상대가 제거되고, 소유 관계와 다른 멤버는 남는다.
- 비소유자의 라벨 이름 변경이 `LabelAuthorizationException`으로 거부된다.
- 보이는 멤버만으로 교체한 뒤 비활성 멤버를 재활성화하면 라벨에 다시 보인다.
- 동시 교체 두 건 후 최종 멤버가 둘 중 한 목록과 정확히 같다.
- 라벨 조회 API의 응답 구조가 변하지 않는다.
