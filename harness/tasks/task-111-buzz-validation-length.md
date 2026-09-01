# Task-111: buzz 입력 길이 검증

> **Domain Change:** [x] — 길이 상한을 도메인에 새긴다. 로직·필드는 건드리지 않는다.
> **FE 협조:** [ ] — 응답 스키마 불변. 초과 입력이 400 + 필드 오류로 바뀐다. 정한 상한을 입력창 `maxlength`로 미러링하면 된다.

## 왜

buzz 본문과 댓글은 **길이가 어디에도 검증되지 않는다.** flag(task-102)와 달리 buzz는 MongoDB 도큐먼트라 **DB 컬럼 폭이라는 우연한 백스톱조차 없어**, 요청 하나로 수 MB 텍스트를 저장할 수 있고 그 buzz를 읽는 모든 경로가 그것을 메모리에 올린다. 500도 나지 않고 조용히 쌓인다.

flag에서 확정한 검증 소유 규칙([[feedback-validation-ownership]])을 적용한다: 길이 상한은 제품 판단이므로 도메인이 원천을 갖고, DTO가 그 상수를 참조한다.

## 상한 (결정됨)

- **본문**(`Buzz.text`) — **1000자**. 캐스팅/공유의 핵심 콘텐츠라 flag 설명(500)보다 넉넉하게.
- **댓글**(`BuzzComment.text`) — **300자**. 6시간이면 사라지는 휘발성·반응성 콘텐츠라 flag comment(500)보다 짧게. 본문의 1/3로 위계를 둔다.

## 알려진 제약

- 초과·blank는 **신규 예외 없이** 기존 `BuzzInvalidStateException`(이미 400)으로 거절한다. flag가 새 예외를 판 것은 기존이 409여서였고 buzz는 해당 없다.
- 영문 기본 문구 override는 task-102의 전역 `ValidationMessages.properties`가 이미 처리했다. 이 task는 인라인 문구만.

## Out of Scope

- `imageKeys` 개수·크기 제한 — 별건.
- 수신자 수 제한(150) — 이미 있음.
- 기존 도큐먼트의 초과 텍스트 소급 정리 — 읽기는 무해, 수정 시 걸린다. 필요하면 별도.

## 관련

task-102(flag 선례), [[feedback-validation-ownership]].
