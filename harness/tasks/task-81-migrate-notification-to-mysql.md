# Task-81: Notification → MySQL 이관

> **상태: 롤백됨**
> 구현 완료 후 Task-82(Buzz 이관) 취소 결정으로 인해 롤백.
> Buzz가 MongoDB를 계속 사용하므로 Notification만 이관해도 MongoDB 의존성이 제거되지 않아 이득이 없었음.

## 배경 및 목적

`notification` 도메인의 알림 데이터가 MongoDB에 저장되고 있다. Notification은 수신자 1명, 단순 필드로 구성된 플랫 구조로 관계형 모델에 자연스럽게 맞으며, TTL 외에 도큐먼트 모델 특성이 없다. MySQL(JPA)로 이관하여 MongoDB 의존성을 줄이는 첫 번째 단계.

## 작업 내용 (롤백 전 구현)

- `Notification` → `@Document`에서 `@Entity`로 전환
- `NotificationMongoRepository` → `NotificationJpaRepository`로 교체
- 30일 TTL → `NotificationCleanupScheduler`(`@Scheduled` + ShedLock)로 대체
- `MongoConfig` notification 패키지 제거

## 롤백 사유

- Task-82(Buzz → MySQL) 취소: Buzz의 임베딩 컬렉션·원자 연산 구조가 관계형 이관 복잡도를 정당화하지 못함
- Notification만 이관 시 MongoDB 커넥션 풀·의존성은 그대로 유지됨 → 실질 이득 없음
