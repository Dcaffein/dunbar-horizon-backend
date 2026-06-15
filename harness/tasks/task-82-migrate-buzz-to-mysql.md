# Task-82: Buzz → MySQL 이관

> **상태: 취소됨**
> Buzz의 도큐먼트 모델(임베딩 수신자·댓글, 원자 연산)이 관계형 모델에 맞지 않고,
> 커넥션 풀 리소스 비용이 이관 복잡도를 정당화하지 못함.
> Task-81(Notification 이관)도 함께 롤백됨.

## 배경 및 목적

`buzz` 도메인의 콘텐츠(`Buzz`, `BuzzComment`)가 MongoDB에 저장되고 있다. Task-81(Notification 이관)에 이어 MongoDB 의존성을 완전히 제거하는 두 번째 단계로, `buzz`를 MySQL(JPA)로 이관한다. 이관 완료 후 MongoDB 커넥션 풀과 관련 의존성 전체를 제거한다.

## 취소 사유

- `Buzz`는 `recipientIds`, `readRecipientIds`, `comments`를 임베딩한 도큐먼트 모델로 설계됨
- `addComment` 등 MongoDB 원자 연산(`$push+$addToSet`)을 MySQL로 옮기면 트랜잭션 복잡도 증가
- 페이징 구현 시 3-쿼리 패턴 + `reconstitute()` 팩토리 등 상당한 인프라 추가 필요
- MongoDB Atlas 비용 및 커넥션 풀 리소스 비용이 해당 복잡도를 정당화하지 못함
