## Objective
단일 문서 내 중첩된 답장 데이터가 늘어날 경우 발생하는 네트워크 페이로드 비대화 및 메모리 병목을 최적화합니다.

## Domain Change
[ ] 없음

## Target Files
- `buzz/adapter/out/persistence/mongo/BuzzMongoTemplateRepository.java` — Slice 쿼리 적용
- `buzz/application/dto/result/BuzzDetailResult.java` — 반환 데이터 구조 확인

## Requirements
1. MongoDB 상세 조회 시 `$slice` 연산자를 사용하여 답장 배열의 일부분만 조회하도록 쿼리를 수정합니다.
2. 클라이언트의 요구와 상관없이 서버에서 결정한 고정 개수(20개)만큼 최신 답장을 반환합니다.

## Decisions
| 항목 | 결정값 | 비고 |
|------|--------|------|
| Slice 고정 크기 | 20 | 모바일 가독성 및 성능 최적화 |
| 조회 정렬 기준 | 최신순 (Last 20) | 휘발성 소통 특성 반영 |