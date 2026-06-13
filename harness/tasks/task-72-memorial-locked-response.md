# Task 72: 후기 조회 응답에 locked 필드 추가

## 배경

후기(Memorial)는 해당 Flag에 본인도 후기를 남긴 경우에만 읽을 수 있다.
현재 API는 후기가 없을 때와 본인이 후기를 안 남겨서 못 볼 때 모두 빈 배열(`[]`)을 반환한다.
프론트엔드는 이 두 상태를 구분할 수 없어 "후기를 남기면 읽을 수 있어요" 안내를 줄 수 없다.

## 목표

`GET /api/v1/flags/{flagId}/memorials` 응답에 `locked` 필드를 추가해
빈 배열의 이유를 프론트엔드가 구분할 수 있게 한다.

```json
// 후기가 없음
{ "memorials": [], "locked": false }

// 후기가 있으나 본인이 안 남겨서 못 봄
{ "memorials": [], "locked": true }

// 정상 조회
{ "memorials": [...], "locked": false }
```

## 설계 결정

### locked 필드 vs 도메인 에러

"후기를 아직 안 남겼다"는 예외가 아닌 **예측된 정상 비즈니스 상태**이므로 4xx가 아닌
응답 필드로 표현한다. 프론트엔드도 에러 핸들러가 아닌 컴포넌트 내에서 자연스럽게 분기할 수 있다.

### 서비스 2단계 분리

현재 `findAllMemorialsIfMemorialized`는 조회 규칙이 SQL에 묻혀있다.
아래 두 단계로 분리해 서비스 코드에서 비즈니스 규칙이 드러나도록 개선한다.

```
existsByFlagId          → 후기가 하나라도 있는가?
existsByFlagIdAndWriterId → 본인이 후기를 남겼는가?
findAllByFlagId         → (위 두 조건 통과 후) 전체 조회
```

`findAllMemorialsIfMemorialized`는 더 이상 필요 없으므로 제거한다.

## 변경 파일 목록

| 파일 | 변경 내용 |
|------|---------|
| `flag/domain/memorial/repository/FlagMemorialRepository.java` | `existsByFlagIdAndWriterId`, `findAllByFlagId` 추가 / `findAllMemorialsIfMemorialized` 제거 |
| `flag/adapter/out/persistence/jpa/FlagMemorialJpaRepository.java` | 동일 (Spring Data 메서드명 자동 생성) |
| `flag/adapter/out/persistence/FlagMemorialRepositoryAdapter.java` | 위임 메서드 교체 |
| `flag/application/dto/result/MemorialListResult.java` | 신규 — `memorials` + `locked` |
| `flag/application/port/in/FlagMemorialQueryUseCase.java` | 반환 타입 `List<MemorialResult>` → `MemorialListResult` |
| `flag/application/service/memorial/FlagMemorialQueryService.java` | 2단계 로직으로 재작성 |
| `flag/adapter/in/web/FlagMemorialController.java` | 응답 타입 교체 |

## Result

(미완료)
