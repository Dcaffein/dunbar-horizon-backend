# Task-42: Encore 호스트 권한 검증 누락 수정

## 배경

`FlagHostService.encoreFlag()`에서 `command.hostId()`(현재 로그인 사용자)와 원본 Flag의 `hostId`를 비교하지 않는다. 어떤 인증된 사용자든 타인의 Flag에 encore를 생성할 수 있다.

## 목표

encore 생성 시 요청자가 원본 Flag의 호스트인지 검증한다.

## 핵심 결정사항

- 검증 위치는 `FlagEncoreCreator.encore()` — 도메인 규칙("호스트만 encore를 만들 수 있다")이므로 도메인 서비스에서 처리한다.
- `FlagAuthorizationException`을 사용한다.

## 브랜치

`ai/fix-encore-host-authorization`

## 결과

**상태:** 완료 (main 머지)

| 커밋 | 내용 |
|------|------|
| `7e37b97` | `FlagEncoreCreator`에 호스트 검증 추가, `FlagEncoreCreatorTest` 신규 (3케이스) |

**변경 내역:**
- `FlagEncoreCreator.encore()` — `parentFlag.getHostId().equals(hostId)` 검증 추가, 불일치 시 `FlagAuthorizationException`
- `FlagEncoreCreatorTest` 신규 — 호스트 성공, 비호스트 예외, 중복 앵콜 예외
