# Task-121: 중복 초대 금지를 DB 제약으로 옮긴다

## Background

`FlagInvitation`은 `Flag`와 공유하는 집합 불변식이 없다. 초대 수에 상한이 없고, 초대 생성에 필요한 Flag 정보(모집 중인지, 호스트가 누구인지, 초대자가 권한을 가졌는지)는 읽기만 하면 된다. 유일한 집합 규칙이 "같은 플래그에 같은 사람을 두 번 초대하지 않는다"인데, 이것은 두 열의 유일성이라 DB 제약으로 표현된다.

그 제약이 실제로는 걸려 있지 않다.

- `V1__baseline.sql`의 `flag_invitations`에는 PK 외에 제약이 없다.
- `V3__add_flag_indexes.sql`의 `idx_flag_invitations_flag_invitee`는 `UNIQUE`가 아닌 일반 인덱스다.
- `FlagInvitationManager.invite`는 `existsByFlagIdAndInviteeId`로 확인한 뒤 생성하는데 Flag 행을 잠그지 않는다. 참여 확정 경로인 `FlagParticipationManager.confirmParticipation`이 `findByIdForUpdate`로 직렬화하는 것과 다르다.

따라서 호스트와 초대 권한을 가진 참여자가 같은 사람을 동시에 초대하면 행이 둘 생긴다. 영향은 초대 목록 중복 노출과 한쪽 수락 후 남는 잔여 초대이고, 정원처럼 사후 보정이 불가능한 종류는 아니다.

`flag_participants`에도 `(flag_id, participant_id)` 유니크가 없다. 이쪽은 참여 경로가 Flag 행을 잠그고 들어가므로 현재는 안전하지만 백스톱이 없다.

## Objective

중복 초대 금지를 DB 제약으로 옮겨 동시 요청에서도 깨지지 않게 한다.

## Decision

### 락이 아니라 제약으로 막는다

참여는 "참여자 수가 정원 이하"를 제약으로 쓸 수 없어 Flag 행 락이 필요했다. 중복 초대는 두 열의 유일성이므로 제약으로 끝난다. 여기에 락을 추가하면 초대가 플래그 단위로 직렬화되어 비용만 는다.

### 사전 확인은 남긴다

`existsByFlagIdAndInviteeId` 검사를 제거하지 않는다. 정상 경로에서는 도메인 예외를 주는 쪽이 낫고, 제약은 경합 상황의 백스톱이다. 제약 위반은 `FlagInvitationDuplicateException`으로 변환해 순차 호출과 동시 호출의 응답을 같게 만든다.

### 기존 중복 행을 먼저 정리한다

제약을 걸기 전에 운영 데이터의 중복 여부를 확인하고, 있으면 가장 먼저 만들어진 행만 남긴다. `flag_invitations`와 `flag_participants` 모두 같다. 정리와 제약 추가는 같은 마이그레이션 파일에 둔다. 중복이 있는 상태에서 제약만 추가하면 마이그레이션이 실패한다.

### `flag_participants`에도 같은 제약을 건다

백스톱 목적이다. 락을 거치지 않는 경로가 나중에 생겨도 중복 참여가 들어가지 않는다.

이것이 "Flag와 FlagParticipant는 같은 애그리거트"라는 판정을 바꾸지는 않는다. 정원 규칙은 여전히 제약으로 표현할 수 없어 락이 필요하고, 유니크 제약이 막는 것은 중복 참여뿐이다.

### `FlagInvitation`의 상속은 바꾸지 않는다

`BaseTimeAggregateRoot`의 값은 도메인이 이벤트를 발행할 수 있다는 데 있다. `FlagInvitationSentEvent`는 `invitationId`를 담는데 이 값은 저장 뒤에야 생기므로 도메인이 발행할 수 없다. 상속만 올리면 얻는 것이 없어 `BaseTimeEntity`를 유지한다.

## Out of Scope

- 초대 만료와 정리 정책
- 참여 정원 로직과 락 범위 변경
- `FlagComment`, `FlagMemorial`의 변경
- 초대 목록 조회 API의 응답 형태

## Acceptance Criteria

- 같은 `flagId`와 `inviteeId`로 동시에 두 번 초대해도 행이 하나만 남고, 나머지는 `FlagInvitationDuplicateException`으로 떨어진다.
- 위 동시성 테스트가 수정 전 동작에서 중복 행 둘을 만드는 것을 확인한다.
- 순차 중복 초대의 예외와 응답이 변경 전과 같다.
- 마이그레이션이 중복 행이 있는 상태에서도 성공하고, 적용 후 중복이 남지 않는다.
- `flag_participants` 유니크 추가 후 기존 참여 경로와 초대 수락 경로 테스트가 통과한다.
- 앙코르 플래그의 일괄 초대 경로가 변경 전과 같이 동작한다.
