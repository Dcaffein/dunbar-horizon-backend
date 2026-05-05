# PLAN — Task-18: Flag 도메인 테스트 추가

## 목표

Flag 도메인(flag, comment, memorial 서브도메인) 전체에 걸쳐 누락된 테스트를 계층별로 추가한다.
도메인 엔티티 단위 테스트 → 서비스 Mockito 단위 테스트 → 컨트롤러 슬라이스 테스트 순서로 작성한다.

---

## 작성 파일 목록

| 파일 | 계층 | 상태 |
|------|------|------|
| `flag/domain/flag/FlagTest.java` | 도메인 | ✅ |
| `flag/domain/comment/FlagCommentTest.java` | 도메인 | ✅ |
| `flag/application/service/flag/FlagQueryServiceTest.java` | 서비스 | ✅ |
| `flag/application/service/flag/FlagHostServiceTest.java` | 서비스 | ✅ |
| `flag/application/service/flag/FlagManagementServiceTest.java` | 서비스 | ✅ |
| `flag/application/service/flag/FlagParticipationServiceTest.java` | 서비스 | ✅ |
| `flag/application/service/comment/FlagCommentQueryServiceTest.java` | 서비스 | ✅ |
| `flag/application/service/comment/FlagCommentCommandServiceTest.java` | 서비스 | ✅ |
| `flag/application/service/memorial/FlagMemorialCommandServiceTest.java` | 서비스 | ✅ |
| `flag/application/service/memorial/FlagMemorialQueryServiceTest.java` | 서비스 | ✅ |
| `flag/adapter/in/web/FlagControllerTest.java` | 컨트롤러 | ✅ |
| `flag/adapter/in/web/FlagCommentControllerTest.java` | 컨트롤러 | ✅ |
| `flag/adapter/in/web/FlagMemorialControllerTest.java` | 컨트롤러 | ✅ |

---

## 브랜치

`ai/feat-flag-domain-tests` (main에서 분기)
