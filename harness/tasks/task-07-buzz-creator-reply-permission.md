## Objective
작성자 본인이 자신의 휘발성 게시물에 상호작용(답장)할 수 없는 논리적 결함을 해결합니다.

## Domain Change
[x] 있음 — `Buzz` 내 답장 작성 권한 로직 수정

## Target Files
- `buzz/domain/Buzz.java` — `createReply` 권한 검증 메서드 수정

## Requirements
1. `createReply` 실행 시 요청자 식별자(replierId)가 수신자 목록(`recipientIds`)에 없더라도, 원본 작성자(`creatorId`)와 일치하면 작성을 허용합니다.
2. 작성자 본인의 답장에 대해서는 별도의 자동 읽음 처리나 상태 변경 이벤트 없이 단순 작성을 허용하는 것으로 유지합니다.

## Decisions
| 항목 | 결정값 | 비고 |
|------|--------|------|
| 작성자 본인 허용 | 있음 (OR 조건) | creatorId == replierId |
| 부가 사이드 이펙트 | 없음 | 기획 의도에 따른 단순화 |