# PLAN: Task-64 — Label.exposure 필드 제거

## Branch
`ai/refactor-remove-label-exposure` (from `main`)

## 목표

`Label` 도메인에서 `exposure` 필드를 전 레이어에 걸쳐 제거한다.

---

## 현황 분석

- `exposure`는 `Label` Neo4j 노드의 프로퍼티로 저장·업데이트되고 있으나, 조회 시 분기에 쓰이는 코드가 없는 dead field다.
- `LabelResult`를 통해 API 응답에도 포함되어 있어 클라이언트에 노출되고 있다.
- `LabelCreateRequest`에 `@NotNull` 필수 검증이 걸려 있어, 현재 라벨 생성 요청에 `exposure` 값이 반드시 필요하다.
- 테스트 코드(`LabelNeo4jRepositoryTest`) 8곳에서 `LabelTestFactory.createLabel(owner, name, true)` 형태로 호출 중이다.

---

## 변경 파일 목록

| 파일 | 변경 내용 |
|------|---------|
| `social/domain/label/Label.java` | `exposure` 필드 제거, 생성자 파라미터 제거, `updateExposure()` 메서드 제거 |
| `social/domain/label/LabelFactory.java` | `create()` 파라미터에서 `exposure` 제거 |
| `social/application/port/in/LabelCommandUseCase.java` | `createLabel()`, `updateLabel()` 시그니처에서 `exposure` 제거 |
| `social/application/service/LabelService.java` | `createLabel()`, `updateLabel()` 파라미터 및 `applyIfPresent(exposure, ...)` 제거 |
| `social/adapter/in/web/LabelController.java` | `dto.exposure()` 전달 제거 (create, update 두 곳) |
| `social/adapter/in/web/dto/LabelCreateRequest.java` | `exposure` 필드 및 `@NotNull` 검증 제거 |
| `social/adapter/in/web/dto/LabelUpdateRequest.java` | `exposure` 필드 제거 |
| `social/application/dto/result/LabelResult.java` | `exposure` 필드 제거, `from()` 매핑 제거 |
| `social/domain/label/LabelTestFactory.java` | `createLabel()` 파라미터에서 `exposure` 제거 |
| `social/adapter/out/LabelNeo4jRepositoryTest.java` | `createLabel(owner, name, true)` → `createLabel(owner, name)` (8곳) |

---

## 구현 방향

- `LabelService.updateLabel()`에서 `exposure` 관련 `applyIfPresent` 라인만 제거한다. `applyIfPresent` 헬퍼 자체는 `labelName` 처리에 여전히 사용되므로 유지한다.
- `LabelUpdateRequest`는 `exposure` 제거 후 `labelName` 단일 필드만 남는다. PATCH 의미상 선택적 단일 필드 요청은 정상적이므로 별도 구조 변경 없이 유지한다.
- Neo4j에 기존 저장된 노드의 `exposure` 프로퍼티는 매핑 코드 제거 시 자연스럽게 무시된다. DB 마이그레이션 불필요.

---

## 예상 사이드 이펙트

- API 클라이언트(프론트엔드)가 라벨 생성 시 `exposure` 필드를 보내고 있다면, 해당 필드는 무시된다. (추가 필드 무시는 Jackson 기본 동작)
- 라벨 조회 응답(`LabelResult`)에서 `exposure` 필드가 사라진다. 프론트엔드에서 이 필드를 사용 중이라면 대응 필요.

---

## 테스트 전략

기본 프로토콜 준수. `LabelNeo4jRepositoryTest`의 호출부 시그니처만 수정하며, 테스트 로직 자체 변경 없음.
