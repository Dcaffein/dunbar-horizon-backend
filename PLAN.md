# PLAN: Task 23 — FlagRole에서 GUEST 제거 및 getMyFlagsByRole 엔드포인트 추가

## 작업 목표
`FlagRole.GUEST`를 제거한다.
`getMyFlagsByRole()`은 유효한 기능이지만 컨트롤러에 노출되지 않은 상태이므로,
`GET /api/v1/flags/me?role={HOST|PARTICIPANT}` 엔드포인트를 추가해 완성한다.

---

## 현황 분석

### GUEST가 쓰이는 위치

| 위치 | 용도 | 조치 |
|------|------|------|
| `FlagRole.java` | enum 값 정의 | 제거 |
| `FlagQueryService.getMyFlagsByRole()` | GUEST 케이스 → `List.of()` | 케이스 제거. HOST/PARTICIPANT exhaustive switch |
| `FlagQueryService.determineViewerRole()` | 폴백으로 GUEST 반환 | `null` 반환으로 변경 |
| `FlagDetailResult.role` 필드 | 응답 DTO | `@Nullable` 추가 |

### `getMyFlagsByRole` 미노출 현황
`FlagQueryUseCase.getMyFlagsByRole()`은 구현되어 있으나 컨트롤러 엔드포인트가 없다.
`FlagQueryController`에 `GET /me?role=...`을 추가한다.
기존 `/me/hosting`, `/me/participating`은 유지한다 (별도 삭제 task).

---

## 변경 파일 목록

**수정**

| 파일 | 변경 내용 |
|------|-----------|
| `flag/application/port/in/FlagRole.java` | `GUEST` 제거. HOST, PARTICIPANT만 유지 |
| `flag/application/service/flag/FlagQueryService.java` | `getMyFlagsByRole()` switch에서 GUEST 케이스 제거. `determineViewerRole()` 반환 타입 `@Nullable FlagRole`, `null` 반환 |
| `flag/application/dto/result/FlagDetailResult.java` | `FlagRole role` 필드에 `@Nullable` 추가 |
| `flag/adapter/in/web/FlagQueryController.java` | `GET /me?role={HOST\|PARTICIPANT}` 엔드포인트 추가. `FlagQueryUseCase.getMyFlagsByRole()` 호출 |
| `test/.../FlagQueryServiceTest.java` | `getMyFlagsByRole_Guest_ReturnsEmpty` 테스트 제거 |
| `test/.../FlagControllerTest.java` | `getMyFlags_HostRole_Returns200` — 기존 테스트가 이미 올바른 형태이므로 유지 |

---

## 구현 방향

```java
// FlagQueryController
@GetMapping("/me")
public ResponseEntity<List<FlagResult>> getMyFlagsByRole(
        @CurrentUserId Long currentUserId,
        @RequestParam FlagRole role) {
    return ResponseEntity.ok(flagQueryUseCase.getMyFlagsByRole(currentUserId, role));
}
```

- `@RequestParam FlagRole role` — Spring이 문자열을 FlagRole로 변환. GUEST가 제거되면 `?role=GUEST` 요청은 자동으로 400 Bad Request.
- `determineViewerRole()` 반환 타입을 `@Nullable FlagRole`로 바꾸고 null이 "관계 없는 뷰어"를 의미하게 한다.

---

## 예상 사이드 이펙트

- `FlagDetailResult.role`이 nullable이 되므로, 클라이언트가 `role == null`을 "관계 없는 뷰어"로 처리해야 한다.
- 기존에 GUEST를 체크하는 프론트엔드 코드가 있다면 수정 필요.
