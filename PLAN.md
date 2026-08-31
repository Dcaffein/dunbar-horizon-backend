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
