# Task Spec Guide

"무엇을, 어떻게"는 Claude가 코드베이스를 읽고 PLAN.md에서 도출합니다.
명세서에는 **왜 이 작업이 필요한가**와 **사람만 알 수 있는 제약**만 적습니다.

## 섹션

| 섹션 | 필수 | 설명 |
|------|------|------|
| Objective | ✓ | 왜 필요한가 (1–3문장) |
| Domain Change | ✓ | domain/ 수정 여부 체크 |
| Background | — | 코드에서 드러나지 않는 결정 배경·외부 제약 |
| Decision | — | 구현 전 사람이 확정해야 할 선택지 |

## 최소 템플릿

```markdown
# Task NN: [제목]

## Objective
(왜 이 작업이 필요한가 — 1–3문장)

## Domain Change
[ ] 없음  [x] 있음
```
