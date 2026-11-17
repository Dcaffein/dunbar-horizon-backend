# Backend Agent Workflow

논의한 작업은 GitHub Issue로 확정하고 실행 시점은 별도로 정한다. Issue는 합의된 작업과 의도의 기록이고, PLAN은 현재 코드에 적용할 실행 계획이다.

## 1. Issue와 작업 맥락 확인

- 지정된 Issue의 본문을 읽고 목적, 요구사항, 설계 결정과 이유, 범위, 완료 조건을 파악한다. 필요한 댓글과 연결된 작업도 확인한다.
- Issue 작성과 갱신은 [ISSUE-GUIDE.md](ISSUE-GUIDE.md)를 따른다. 중요한 합의는 댓글에만 남기지 않는다.
- `CLAUDE.md`의 문서 지도에서 필요한 지침을 확인한다.
- 이전한 작업의 원본과 GitHub Issue 번호는 [이전 Task 기록](#이전-task-기록)에서 찾을 수 있다.

## 2. 코드 탐색과 설계 재논의

- 관련 코드와 테스트를 읽어 Issue의 전제가 현재 코드와 맞는지 확인한다. 변경 범위, 호출자, 데이터와 동시성에 미치는 영향을 파악한다.
- 확정된 요구사항·설계·범위를 번복해야 한다면 PLAN 수립을 중단한다. 기존 결정, 충돌하는 코드 근거, 유지할 경우의 문제, 영향과 대안을 보고한다.
- 사용자와 Issue 재논의 단계로 전환한다. 다른 세션에서 논의할 수 있도록 근거와 쟁점을 전달할 수 있게 정리한다.
- 합의 후 Issue를 갱신하는 것까지가 이 단계의 결과다. 갱신을 PLAN 재작성이나 구현 재개의 승인으로 간주하지 않는다. 사용자가 재개를 지시하면 갱신된 Issue와 현재 코드를 기준으로 계획을 수립한다.
- 구현 중 같은 문제가 발견되어도 이 절차를 적용하고 해당 결정에 의존하는 구현을 진행하지 않는다.

## 3. 실행 계획과 승인

- [PLAN-GUIDE.md](PLAN-GUIDE.md)에 따라 로컬 `PLAN.md`를 작성한다. Issue의 배경과 설계를 반복하지 않고 커밋별 변경·검증·부작용을 정리한다.
- 실행 계획과 미해결 사항을 보고하고 구현 승인을 받는다.
- 승인은 지시한 범위와 단계에 적용한다. 설계 방향에 대한 동의만으로 구현이나 커밋까지 승인된 것으로 보지 않는다.
- 사용자가 해당 범위의 구현·커밋을 명시적으로 지시했다면 같은 승인을 반복 요청하지 않는다. 단, 커밋별 승인을 요구했다면 각 커밋 전에 승인받는다. 승인 범위를 벗어나는 변경은 다시 보고한다.

## 4. 브랜치 생성

- 승인 후 기본 브랜치에서 작업 브랜치를 생성한다. 다른 미병합 작업에 의존하면 기준 브랜치와 이유를 계획에 명시하고 합의한다.
- 브랜치 이름은 `ai/[작업 카테고리]-[기능명]`으로 한다. 기능명은 Issue의 작업을 표현한다. 예: `ai/refactor-domain-repository-boundary`.
- 이미 해당 작업용 브랜치나 워크트리에서 시작했다면 중복 분기하지 않는다. 기존 작업과 무관한 변경을 덮어쓰거나 커밋에 포함하지 않는다.

## 5. 구현과 검증

- 승인된 계획에 따라 커밋 단위 하나씩 구현한다.
- 각 단위에 필요한 테스트를 작성·실행한다. [TESTING-GUIDE.md](TESTING-GUIDE.md)를 따른다. 문서만 바꾸는 등 테스트가 불필요한 작업은 적절한 문서·diff 검증으로 대신하고 보고한다.
- 실행 순서나 변경 범위가 조정되면 PLAN을 갱신한다. Issue의 합의를 바꾸는 경우에는 2단계의 재논의를 거친다.
- 아키텍처 규칙이 바뀌면 해당 구현과 같은 커밋 단위에서 `ARCHITECTURE.md`를 갱신한다. 미구현 설계를 현재 규칙으로 먼저 기록하지 않는다.
- 컴파일이나 테스트 오류는 원인을 분석해 수정한다. 동일한 오류의 해결에 3회 이상 실패하면 코드 수정을 중단하고 상황을 보고한 뒤 지시를 기다린다.

## 6. 커밋 보고와 승인

- 커밋 전에 다음을 보고한다.
  - 해결한 문제와 실제 변경
  - 승인된 실행 계획에서 달라진 점과 이유
  - 검증 결과, 테스트 클래스별 개수와 남은 제한
  - 버그 수정이면 회귀 테스트가 수정 전 동작에서 실패하는지 확인한 결과
  - 커밋 메시지 초안 전문
- 승인받은 단위의 파일만 스테이징해 커밋한다. 다음 단위는 해당 단위의 커밋 후 진행한다. 승인 범위는 3단계의 기준을 따른다.
- 커밋이나 PR에서 관련 Issue를 연결한다. 실제 변경과 이유는 해당 구현 커밋에 기록하고, 계속 갱신되는 PLAN의 최초 상태를 완료 시점에 복원하도록 요구하지 않는다.

## 7. 병합과 Issue 완료

- 모든 단위를 구현한 뒤 Issue의 완료 조건을 확인하고 실제 변경·검증 결과·관련 커밋 또는 PR을 보고한다. 합의한 중요한 변경 이유가 Issue에 반영되어 있는지도 확인한다.
- 구현과 검증이 끝나도 기본 브랜치에 병합되기 전에는 Issue를 닫지 않는다. 이 상태는 "구현 완료, 병합 대기"로 보고한다.
- 병합은 사용자의 승인 범위에 따라 진행한다. 기본 브랜치 병합과 완료 조건 충족을 확인한 뒤 Issue를 닫는다. 일부 구현만 병합된 경우에는 닫지 않는다.
- 닫힌 Issue는 당시 배경과 의사결정 기록으로 보존한다. 로컬 TASK를 `done/`으로 이동하는 완료 절차는 사용하지 않는다.
- 후속 작업은 별도 Issue로 기록하고 연결한다. 구현 결과를 발주서 본문에 작업 일지로 반복하지 않는다.

## 이전 Task 기록

[완료 기록](https://github.com/Dcaffein/dunbar-horizon-backend/tree/5b47eac255f2873471d313f08989457f9291a532/harness/done)과 [취소 기록](https://github.com/Dcaffein/dunbar-horizon-backend/tree/5b47eac255f2873471d313f08989457f9291a532/harness/cancelled)은 Issue 전환 시점의 Git 커밋에 보관한다. 당시 미완료 작업은 아래 GitHub Issue에서 이어간다.

| 보관 원본 | GitHub Issue |
|---|---|
| [Task-126](https://github.com/Dcaffein/dunbar-horizon-backend/blob/5b47eac255f2873471d313f08989457f9291a532/harness/tasks/task-126-buzz-presigned-url-storage.md) | [#1](https://github.com/Dcaffein/dunbar-horizon-backend/issues/1) |
| [Task-142](https://github.com/Dcaffein/dunbar-horizon-backend/blob/5b47eac255f2873471d313f08989457f9291a532/harness/tasks/task-142-buzz-to-mysql.md) | [#2](https://github.com/Dcaffein/dunbar-horizon-backend/issues/2) |
| [Task-143](https://github.com/Dcaffein/dunbar-horizon-backend/blob/5b47eac255f2873471d313f08989457f9291a532/harness/tasks/task-143-notification-to-mysql.md) | [#3](https://github.com/Dcaffein/dunbar-horizon-backend/issues/3) |
| [Task-148](https://github.com/Dcaffein/dunbar-horizon-backend/blob/5b47eac255f2873471d313f08989457f9291a532/harness/tasks/task-148-social-data-cutover.md) | [#4](https://github.com/Dcaffein/dunbar-horizon-backend/issues/4) |
| [Task-155](https://github.com/Dcaffein/dunbar-horizon-backend/blob/5b47eac255f2873471d313f08989457f9291a532/harness/tasks/task-155-domain-facts-from-application.md) | [#5](https://github.com/Dcaffein/dunbar-horizon-backend/issues/5) |
| [Task-156](https://github.com/Dcaffein/dunbar-horizon-backend/blob/5b47eac255f2873471d313f08989457f9291a532/harness/tasks/task-156-domain-errors-http-mapping.md) | [#6](https://github.com/Dcaffein/dunbar-horizon-backend/issues/6) |
