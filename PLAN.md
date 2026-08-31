# PLAN — Friend Request 수신 상태 조회 완화

## 1. 작업 목표

`received` 친구 요청 조회에서 `PENDING`과 `HIDDEN`만 허용하던 예외 검증을 제거한다. `sent`의 status 금지는 유지해, 보낸 사람이 수신자의 숨김 처리를 탐색할 수 없게 한다.

## 2. 현황 분석

- `FriendRequestQueryService`는 `received` 요청에서 status가 생략되면 `PENDING`을 기본값으로 사용한다.
- 현재는 `PENDING`, `HIDDEN` 외 상태를 전달하면 `FriendRequestInvalidException`을 던져 400을 반환한다.
- 수락된 요청은 수락 흐름에서 삭제되므로 `received&status=ACCEPTED`는 제한을 제거하면 빈 목록을 반환한다.
- `sent`는 status를 받지 않고 저장소에서 `requesterId + PENDING`만 조회한다. status 파라미터 금지는 보낸 사람이 `HIDDEN` 상태를 탐색하지 못하게 하는 개인정보 규칙이므로 유지한다.

## 3. 변경 파일

| 파일 | 변경 |
|---|---|
| `src/main/java/com/example/DunbarHorizon/social/application/service/FriendRequestQueryService.java` | `received` status whitelist 예외를 제거하고 전달된 enum 상태로 수신 요청을 조회한다. sent 분기와 기본 PENDING 정책은 유지한다. |
| `src/test/java/com/example/DunbarHorizon/social/application/FriendRequestQueryServiceTest.java` | `received + ACCEPTED`가 예외 없이 receiver/status 조건으로 조회되는지 검증한다. 기존 예외 테스트는 변경한다. |

## 4. 구현 방향

- `queryStatus`는 계속 `status == null ? PENDING : status`로 결정한다.
- `direction == SENT`일 때의 status 거부와 `findSentRequests()`의 `PENDING` 고정 조건은 변경하지 않는다.
- `RECEIVED`는 `queryStatus`를 그대로 `findAllByReceiver_IdAndStatus`에 전달한다.
- API의 잘못된 enum 문자열은 기존 Spring binding 오류 처리에 맡긴다.

## 5. 예상 사이드 이펙트

- `GET /api/v1/friend-requests?direction=received&status=ACCEPTED`의 결과가 400에서 200 빈 배열(현재 데이터 모델 기준)로 변경된다.
- 숨김 요청 탐색 방지 정책은 `sent` 분기와 PENDING 고정 저장소 조회로 그대로 보존된다.
- API URL, request/response DTO, domain 상태 전이 규칙은 변경하지 않는다.

## 6. 테스트 전략

사용자가 코드 완료를 확인한 뒤 승인하면 다음 테스트를 실행한다.

```powershell
$env:JAVA_HOME='C:\\Users\\TFX5470H\\.jdks\\corretto-17.0.15'
$env:Path="$env:JAVA_HOME\\bin;$env:Path"
.\\gradlew.bat test --no-daemon --rerun-tasks --tests '*FriendRequestQueryServiceTest'
```

수신 `PENDING` 기본값, `HIDDEN` 조회, `ACCEPTED` 전달 조회, sent status 거부 및 sent PENDING 고정 조회를 검증한다.

---

# PLAN — 프로젝트 README 작성 (2026-08-31)

## 작업 목표와 승인 범위

사용자가 로컬 프로젝트의 `README.md` 작성을 명시적으로 요청했다.
앞서 제안한 서비스 소개, 바로가기, 대표 사례, 구조, 실행·테스트, 한계의 구성으로 작성한다.
이 문서 작업만 승인된 범위이며, Outbox 동작과 장애 테스트 구현, 공개 Notion 갱신, 원격 푸시는 포함하지 않는다.

## 현황 분석

- 저장소 루트에 README가 없고 상세 기술서는 외부 Notion에 있다.
- Java 21과 Gradle Wrapper를 사용한다. 단위 테스트와 Testcontainers 기반 DB 테스트가 있다.
- 전체 애플리케이션 실행에는 DB·Redis와 OAuth·메일·Firebase·S3 설정이 필요하다.
- 루트 `docker-compose.yml`은 ECR 이미지와 외부 네트워크를 사용하는 배포용 파일이다.
- Outbox의 수신 처리와 재시도 단위 테스트는 있지만, 두 DB의 커밋 경계 및 장애 복구 통합 검증은 아직 없다.

## 변경 파일과 구현 방향

| 파일 | 변경 |
|---|---|
| `README.md` | 소개, 기술 문서와 코드·테스트 연결, 저장소 구조, 실행·테스트 전제조건과 현재 한계를 작성한다. |
| `PLAN.md` | 기존 작업 기록을 보존하고 이번 문서 작업 범위를 추가한다. |

성능 수치는 합성 부하 조건과 함께 적고, 검증되지 않은 장애 복구·실사용자·협업 경험을 주장하지 않는다.
프로덕션 자격증명이나 로컬 개인 경로를 문서에 포함하지 않는다.

## 영향 및 검증

- 실행 코드·설정·API의 변경은 없다.
- 문서의 저장소 내부 링크와 실행 명령을 실제 파일과 대조한다.
- Docker 없이 실행하도록 안내한 기존 단위 테스트 명령만 확인하며, 전체 통합 테스트나 배포는 실행하지 않는다.

## 검증 결과

- README의 저장소 내부 링크 25개가 모두 존재하는 파일 또는 디렉터리를 가리킴을 확인했다.
- 설치된 JDK 21을 사용해 README의 단위 테스트 명령을 실행했다. `FriendshipTest` 7개와 `SocialNetworkExposurePolicyTest` 2개, 총 9개가 실패·오류 없이 통과했다.
- 전체 테스트·DB 장애 주입·배포는 실행하지 않았다. 실행 코드와 기존 테스트 코드는 변경하지 않았다.
- 별도 문서 검토에서 기술 스택·실행 전제조건·검증 범위와 실제 코드의 일치를 확인했다.

---

# PLAN — User Sync 커밋 순서 수정과 코드 안내 README (2026-08-31)

## 작업 목표와 승인 범위

사용자의 후속 지시에 따라 Neo4j 커밋 순서를 먼저 수정한 뒤 장애·유실·재처리 테스트를 붙인다.
README는 헥사고날 구조, 코드 위치, 요청 및 이벤트 흐름을 설명하는 개발자 안내로 개편한다.
도메인 정책, 이벤트 스키마, 재시도 횟수·주기, 배포 구성은 변경하지 않는다. 원격 푸시는 하지 않는다.

## 현황 분석

- `SocialUserEventListener.onUserSync()`의 Neo4j 트랜잭션은 메서드 반환 후 커밋된다.
- 현재 메서드 내부의 완료 이벤트가 MySQL 완료 처리를 동기 호출하므로 Neo4j 커밋보다 먼저 COMPLETED가 저장될 수 있다.
- 메서드 내부 catch는 프록시가 수행하는 커밋 실패를 잡지 못한다.
- 기존 Mockito 테스트는 실제 Spring 프록시와 두 DB의 커밋 경계를 검증하지 않는다.
- 기존 README는 사례 소개 비중이 높고 포트·어댑터의 실제 연결과 실행 흐름 안내가 부족하다.

## 변경 파일과 구현 방향

| 파일 | 변경 |
|---|---|
| `social/application/service/SocialUserSyncCommandService.java` | 기존 그래프 변경을 이동하고 Neo4j REQUIRES_NEW 트랜잭션을 책임진다. |
| `social/application/eventListener/SocialUserEventListener.java` | 비동기 수신 → 별도 서비스 프록시 호출 → 성공 반환 후 완료 이벤트 발행. |
| `account/application/eventListener/UserOutboxEventListener.java` | 완료 처리를 명시적인 MySQL REQUIRES_NEW 트랜잭션으로 수행한다. |
| 관련 단위 테스트 | 그래프 변경과 이벤트 조율 책임에 맞게 분리하고 실패 시 완료 미발행을 검증한다. |
| 교차 DB 통합 테스트 및 `support` 테스트 지원 | 커밋 전 대기·실패, 최초 이벤트 유실, 완료 이벤트 유실과 실제 재시도를 검증한다. |
| `README.md` | 실제 패키지와 포트·어댑터 연결, HTTP 및 Outbox 흐름, 실행·테스트 안내를 중심으로 재구성한다. |

## 영향과 테스트 전략

- 완료 처리는 Neo4j 커밋 성공 이후에만 실행된다. 완료 처리 실패·유실 시 PENDING이 남아 기존 재시도의 대상이 된다.
- 그래프 처리 예외는 트랜잭션 밖에서 잡으므로 Neo4j 롤백이 먼저 일어난다.
- 기존 슬라이스 테스트의 자동 롤백은 AFTER_COMMIT 검증에 부적합하다. `support/TestContainerConfig`를 재사용하는 좁은 Spring 통합 컨텍스트에서 실제 트랜잭션을 커밋한다.
- 장애 주입은 테스트 전용 코드에만 두고, 실제 저장소와 Spring 프록시를 사용한다. 커밋 경계 예외와 이벤트 전달 누락을 재현하며 실제 네트워크 단절·프로세스 종료를 검증했다고 주장하지 않는다.
- 비동기 작업은 명시적인 완료 신호와 제한 시간으로 기다린다. 테스트가 만든 데이터만 정리한다.
- README 내부 링크와 설명을 실제 코드와 대조한다.

## 구현 및 검증 상태

- Neo4j 트랜잭션을 별도 서비스로 분리했고, 서비스 프록시 성공 반환 후에만 완료 이벤트를 발행하도록 변경했다. 완료 처리는 명시적인 MySQL REQUIRES_NEW를 사용한다.
- 기존 그래프 동기화 단위 테스트 8개를 서비스 책임으로 옮기고, 리스너 조율 테스트 4개를 추가했다. Outbox 기록 5개·재시도 2개와 합쳐 관련 단위 테스트 19개가 통과했다.
- 실제 커밋 전 대기, 커밋 직전 예외와 재시도, 최초 이벤트 유실, 완료 이벤트 유실과 중복 방지의 통합 테스트 4개를 작성했다. `support`의 전용 Base 및 좁은 Spring 컨텍스트를 사용하며 공용 Testcontainers를 재사용한다.
- 통합 테스트를 포함한 전체 테스트 소스 컴파일은 성공했다. 선택한 23개 중 단위 19개는 통과했으나, 통합 4개는 Docker 엔진 미연결로 Testcontainers 컨텍스트 초기화에서 실패했다. 실제 DB assertion과 복구 검증은 미실행 상태다.
- README를 코드 구조, 포트·어댑터 매핑, HTTP 조회·변경 흐름, 사용자 동기화 흐름 중심으로 개편했다. 테스트 미검증 범위를 구분해 명시했다.
- 독립 검토에서 실행 코드의 커밋·예외 경계와 README 주요 설명을 확인했다. Docker 준비 후 통합 테스트를 재실행하고 통과한 뒤 커밋한다. 현재 변경은 미커밋 상태다.
