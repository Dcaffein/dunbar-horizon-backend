# Task-35: FlagEncoreCreator 이중 검증 제거

## 배경

`FlagEncoreCreator.encore()`와 `Flag.createEncore()` 양쪽에 동일한 `isEnded()` 검증이 존재한다.

```java
// FlagEncoreCreator.encore()
if (!parentFlag.isEnded()) {
    throw new FlagInvalidStatusException("종료된 플래그만 앵코르를 생성할 수 있습니다.");
}

// Flag.createEncore() — 동일한 검증 중복
if (!this.isEnded()) {
    throw new FlagInvalidStatusException("종료된 플래그만 앵코르를 생성할 수 있습니다.");
}
```

Flag 내부 상태를 확인하는 책임은 Flag에 있으므로 `Flag.createEncore()` 안에서만 검증하고,
`FlagEncoreCreator`는 repository 의존 검증(`existsByParentId`)만 담당하면 된다.

추가로 `FlagMemorialCreator`의 예외 메시지가 영어로 작성되어 코드베이스 일관성이 깨져 있어 함께 수정한다.

## 목표

- `FlagEncoreCreator.encore()`에서 `isEnded()` 중복 검증 제거
- `FlagMemorialCreator` 영어 예외 메시지 → 한국어로 수정

## 변경 파일

| 파일 | 변경 내용 |
|------|----------|
| `flag/domain/flag/FlagEncoreCreator.java` | `!isEnded()` 체크 제거 |
| `flag/domain/memorial/FlagMemorialCreator.java` | 영어 예외 메시지 한국어로 변경 |

## 체크리스트

- [ ] `FlagEncoreCreator.java` — `isEnded()` 중복 검증 제거
- [ ] `FlagMemorialCreator.java` — 예외 메시지 한국어 변환
- [ ] 테스트 코드 확인 (FlagEncoreCreator 관련 테스트)
- [ ] 빌드 확인

## 브랜치

`ai/fix-encore-duplicate-validation`
