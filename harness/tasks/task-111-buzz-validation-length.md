# Task-111: buzz 입력 길이 검증

> **Domain Change:** [x] — 길이 상한을 도메인에 새긴다. 로직·필드는 건드리지 않는다.
> **FE 협조:** [ ] — 응답 스키마 불변. 초과 입력이 400 + 필드 오류로 바뀐다. 정한 상한을 입력창 `maxlength`로 미러링하면 된다.

## 왜

buzz 본문과 댓글은 **길이가 어디에도 검증되지 않는다.** flag(task-102)와 달리 buzz는 MongoDB 도큐먼트라 **DB 컬럼 폭이라는 우연한 백스톱조차 없어**, 요청 하나로 수 MB 텍스트를 저장할 수 있고 그 buzz를 읽는 모든 경로가 그것을 메모리에 올린다. 500도 나지 않고 조용히 쌓인다.

검증 소유 규칙([[feedback-validation-ownership]]) 적용: 길이 상한은 제품 판단이므로 도메인이 원천.

## 상한 (결정됨)

- **본문**(`Buzz.text`) — **1000자**.
- **댓글**(`BuzzComment.text`) — **300자**. 6시간이면 사라지는 반응성 콘텐츠라 본문의 1/3.

## Out of Scope

- `imageKeys` 개수·크기 제한.
- 수신자 수 제한(150) — 이미 있음.
- 영문 기본 문구 override — task-102의 전역 `ValidationMessages.properties`가 이미 처리.
- 기존 도큐먼트의 초과 텍스트 소급 정리.

## 관련

task-102(flag 선례), [[feedback-validation-ownership]].
