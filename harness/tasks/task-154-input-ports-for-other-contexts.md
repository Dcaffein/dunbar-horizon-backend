# Task-154: 입력 포트는 다른 컨텍스트에 공개하는 기능에만 둔다

## Background

`ARCHITECTURE.md` §1은 `application/port/in`에 UseCase 인터페이스를 두라고 정한다. 그래서 모든 모듈의 컨트롤러가 서비스 대신 UseCase를 주입받는다. 2026-11-17 기준 입력 포트는 25개이고(`flag`의 `FlagRole`은 인터페이스가 아니라 포트 패키지에 놓인 타입이다), 다른 모듈에서 쓰는 것은 셋뿐이다.

| 포트 | 다른 모듈의 호출자 |
|---|---|
| `account` `UserQueryUseCase` | `buzz/adapter/out/social/BuzzSocialAdapter`, `flag/adapter/out/user/FlagUserAdapter`, `trace/application/TraceEventListener` |
| `social` `FriendshipQueryUseCase` | `buzz/adapter/out/social/BuzzSocialAdapter`, `buzz/adapter/out/social/ManualRecipientStrategyAdapter`, `flag/adapter/out/user/FlagUserAdapter` |
| `social` `LabelQueryUseCase` | `buzz/adapter/out/social/LabelRecipientStrategyAdapter` |
| `social` `SocialExpansionQueryUseCase` | `buzz/adapter/out/social/AnchorRecipientStrategyAdapter` |

나머지는 구현체 하나와 같은 모듈의 호출자(대부분 컨트롤러)뿐이다. 갈아 끼울 구현도, 숨길 대상도 없어 읽는 단계만 하나 늘린다. 반대로 다른 모듈이 쓰는 포트와 내부용 포트가 구분되지 않아, 어느 시그니처가 모듈 간 약속인지 코드에서 드러나지 않는다.

task-151에서 social `FriendRequestUseCase`를 검토하다 나온 문제다. 한 모듈만 바꾸면 규칙이 모듈마다 갈리므로 전 모듈을 한 번에 다룬다.

## Objective

입력 포트는 다른 바운디드 컨텍스트가 호출하는 기능에만 남는다. 같은 모듈 안의 컨트롤러, 이벤트 리스너, 스케줄러는 서비스를 직접 주입받는다. 남은 UseCase는 그 컨텍스트가 바깥에 공개한 기능 목록이 되고, `ARCHITECTURE.md` §1이 이 기준으로 바뀐다.

## Decision

- 입력 포트는 그 컨텍스트가 다른 컨텍스트에 공개하는 API다. 다른 모듈에서 호출하지 않는 기능에는 두지 않는다.
- 다른 모듈에서 쓰는 포트는 남기되, 내부 호출자도 그 포트를 통해 부를지 서비스를 직접 부를지는 PLAN에서 정한다.
- 컨트롤러 테스트는 UseCase 대신 서비스를 `@MockitoBean`으로 목 처리한다. `BaseControllerTest`가 공통 목을 들고 있다.
- 기각한 대안
  - 지금처럼 모든 유스케이스에 포트를 둔다. 구현 하나·호출자 하나인 인터페이스가 대부분이라 얻는 것이 없다.
  - social만 먼저 바꾼다. 한 프로젝트 안에서 모듈마다 규칙이 갈리고, 바꾼 곳과 아직 안 바꾼 곳이 구분되지 않는다.

## Out of Scope

- 호출하는 쪽의 출력 포트와 어댑터(`buzz/adapter/out/social`, `flag/adapter/out/user` 등). 지금 구조를 유지한다.
- 서비스를 나누는 기준과 서비스 이름. §1의 service 기준은 task-151에서 고쳤다.
- social 조회용 사용자 엔티티. task-153에서 다룬다.

## Acceptance Criteria

- 남은 `application/port/in` 인터페이스는 모두 다른 모듈에 호출자가 있다.
- 같은 모듈의 컨트롤러는 서비스를 직접 주입받는다. 경로, 본문, 상태 코드는 바뀌지 않는다.
- `ARCHITECTURE.md` §1의 `port/in` 설명이 새 기준으로 바뀐다.
- 컨트롤러 테스트와 다른 모듈에서 포트를 쓰는 경로의 테스트가 통과한다.
