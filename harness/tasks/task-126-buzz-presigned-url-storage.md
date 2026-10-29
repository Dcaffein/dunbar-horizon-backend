# Task-126: buzz가 도큐먼트에 저장한 presigned URL을 키로 되돌린다

## Background

`S3ImageUrlResolver.PRESIGN_TTL`은 1시간인데 `Buzz.expiresAt`은 생성 + 6시간이다. buzz는 presigned GET URL을 만들어 도큐먼트에 그대로 저장하므로, 버즈가 사는 6시간 중 뒤의 5시간은 이미지가 열리지 않는다.

저장되는 자리가 넷이다. `Buzz.imageUrls`와 `BuzzComment.imageUrls`는 서비스가 `imageStoragePort.resolveUrls(imageKeys)` 결과를 넣는다. `Buzz.creatorProfileImageUrl`과 `BuzzComment.commenterProfileImageUrl`은 `BuzzSocialAdapter.getCreatorProfiles`가 `UserQueryUseCase.getUserProfiles`에서 이미 서명된 URL을 받아 그대로 저장한다.

읽기 경로는 저장된 문자열을 그대로 내보내고 다시 서명하지 않는다. 그래서 상세 조회 한 응답 안에서 엇갈린다. 수신자 프로필은 매번 새로 받아오므로 멀쩡한데 작성자 프로필은 1시간 뒤 깨진다. 프로필 이미지는 모든 버즈에 들어가므로 사실상 모든 버즈 상세 화면이 해당된다.

## Objective

buzz가 도큐먼트에 저장하는 값을 presigned URL에서 S3 object key로 바꾼다. 서명은 응답을 만드는 시점에 한다. 버즈와 댓글의 이미지가 버즈 수명 6시간 내내 열린다.

## Decision

### 서명 수명 1시간은 그대로 둔다

presigned URL은 값이 아니라 유효기간이 박힌 출입증이다. 읽을 때마다 새로 서명하면 1시간으로 충분하다. 수명을 6시간으로 늘려 맞추는 쪽은 기각한다. 유출 시 열려 있는 시간이 그만큼 길어지고, 버즈 수명이 바뀌면 다시 어긋난다.

### 저장소 안의 선례에 맞춘다

account는 `UserQueryService.resolveProfileUrl`이, social은 `SocialProfileResult`와 `FriendshipDetailResult`가 응답을 만들 때 `resolveUrl`을 부른다. flag는 프로필을 저장하지 않고 요청마다 받아 쓴다. buzz만 반대로 하고 있으니 새 방식을 만들지 않는다.

### 기존 데이터 마이그레이션은 하지 않는다

`Buzz`의 TTL이 6시간이라 배포 후 6시간이면 옛 도큐먼트가 모두 사라진다. `S3ImageUrlResolver.resolveUrl`에 저장된 presigned URL에서 경로를 뽑아 다시 서명하는 분기도 이미 있다.

## Out of Scope

- account, social, flag의 이미지 처리
- 업로드 presign 흐름(`presignUploads`)과 S3 키 형식

## Acceptance Criteria

- 이미지가 들어간 버즈를 만든 뒤 서명 수명이 지나도 상세 조회 응답의 이미지 URL이 유효하다.
- 버즈 상세 응답에서 작성자 프로필과 수신자 프로필이 같은 방식으로 서명된다. 댓글 이미지와 댓글 작성자 프로필도 같다.
- 저장된 `Buzz` 도큐먼트의 어느 필드에도 `X-Amz-`가 들어간 문자열이 없다.
- 위 회귀 테스트가 수정 전 동작에서 실패하는 것을 확인한다.
- 프로필 값을 도큐먼트에 복사할지 요청마다 받을지 정하고 그 판단을 PLAN에 남긴다.
