# PLAN: Task-37 — 이메일로 유저 검색 API 추가

## 작업 목표

`GET /api/v1/users/search?email=` 엔드포인트를 account 도메인에 추가한다.
ACTIVE 유저만 반환하며, 응답은 `id`, `nickname`, `profileImage`만 포함한다.

---

## 변경 파일

| 파일 | 변경 내용 |
|------|----------|
| `account/application/port/in/UserQueryUseCase.java` | `findActiveUserByEmail(String email)` 추가 |
| `account/application/service/UserQueryService.java` | 위 메서드 구현 |
| `account/adapter/in/web/UserController.java` | 신규 — `GET /api/v1/users/search?email=` |

`UserNotFoundException`은 기존 것을 재사용한다.

---

## 구현 방향

### 1. UserQueryUseCase

```java
Optional<UserProfileInfo> findActiveUserByEmail(String email);
```

### 2. UserQueryService

`UserRepository.findByEmail()`로 조회 후 `ACTIVE` 상태 확인.
ACTIVE 유저가 없으면 `UserNotFoundException` throw.

```java
@Override
public Optional<UserProfileInfo> findActiveUserByEmail(String email) {
    return userRepository.findByEmail(email)
            .filter(user -> user.getStatus() == UserStatus.ACTIVE)
            .map(UserProfileInfo::from);
}
```

### 3. UserController (신규)

```java
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @GetMapping("/search")
    public ResponseEntity<UserProfileInfo> searchByEmail(
            @CurrentUserId Long currentUserId,
            @RequestParam String email) {

        UserProfileInfo result = userQueryUseCase.findActiveUserByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("해당 이메일로 등록된 사용자를 찾을 수 없습니다."));
        return ResponseEntity.ok(result);
    }
}
```

---

## 브랜치

`ai/feat-user-search-by-email`
