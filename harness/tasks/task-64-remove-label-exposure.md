# Task 64 — Label.exposure 필드 제거

## Objective

`Label` 도메인에서 `exposure` 필드를 완전히 제거한다.

## 배경 및 이유

- 라벨은 순수하게 **개인 정보**다. 유저가 다른 유저에게 붙이는 사적인 메타데이터이며, 외부에 공개되는 정보가 아니다.
- 라벨은 `AnchorExpansion` 등 네트워크 조회에 활용되지만, 이는 서버 내부 로직이며 `exposure` 여부와 무관하다.
- 공개 범위 제어는 `Friendship.isRoutable`이 이미 담당하고 있다. `exposure`는 중복·충돌되는 개념이다.
- 원래 `exposure`는 라벨이 네트워크 조회에 사용될 때 해당 라벨을 조회에 포함할지 여부를 결정하는 용도였으나, 이 의미가 직관적이지 않다. 프라이버시 제어는 `Friendship.isRoutable`(해당 친구를 타인의 네트워크에 노출할지 여부)로 일관되게 관리하는 것이 더 명확하다.
- 현재 `exposure`는 저장·업데이트만 될 뿐, **읽어서 분기하는 코드가 없는 dead field**다.

## Out of Scope

- `isRoutable` 관련 로직 변경 없음
- 라벨 기능 자체의 구조 변경 없음
- 다른 도메인 변경 없음

## 변경 대상 (탐색 전 예비 목록)

| 파일 | 예상 변경 |
|------|---------|
| `social/domain/label/Label.java` | `exposure` 필드, 생성자 파라미터, `updateExposure()` 제거 |
| `social/domain/label/LabelFactory.java` | `exposure` 파라미터 제거 |
| `social/application/service/LabelService.java` | `exposure` 파라미터 제거 |
| `social/application/port/in/LabelCommandUseCase.java` | `exposure` 파라미터 제거 |
| `social/adapter/in/web/LabelController.java` | `exposure` 전달 제거 |
| `social/adapter/in/web/dto/LabelCreateRequest.java` | `exposure` 필드 제거 |
| `social/adapter/in/web/dto/LabelUpdateRequest.java` | `exposure` 필드 제거 |
| `social/application/dto/result/LabelResult.java` | `exposure` 필드 제거 |
| `social/domain/label/LabelTestFactory.java` | `exposure` 파라미터 제거 |

## Edge Cases

- Neo4j에 이미 `exposure` 프로퍼티가 저장된 노드가 있더라도, 해당 필드를 매핑하는 코드가 사라지면 자연스럽게 무시된다. DB 마이그레이션은 불필요하다.
