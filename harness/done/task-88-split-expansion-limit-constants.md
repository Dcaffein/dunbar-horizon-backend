# Task-88: SocialExpansionQueryService limit 상수 분리 및 친구 추천 상한 조정

## 배경

`SocialExpansionQueryService`는 두 유즈케이스에서 동일한 limit 상수를 공유한다.

| 메서드 | 유즈케이스 | UI |
|--------|-----------|-----|
| `getAnchorExpansion` | Buzz 수신자 선택 (내 친구 대상) | 리스트 |
| `getRecommendationsByAnchor` | 친구 추천 (2-hop 모르는 사람) | 네트워크 지형도 |

두 유즈케이스는 성격이 근본적으로 다르다.

- **Buzz 수신자**: 아는 사람, 리스트 UI → 150 상한 무방
- **친구 추천**: 모르는 사람, 네트워크 지형도 → 노드 수 과다 시 엣지 겹침, 인지 부하 급증

현재 MAX_LIMIT=150이 두 케이스에 동일하게 적용되어 친구 추천 시 지형도에 과도한 노드가 표시된다.

---

## 결정 근거

### limit 상한 결정

`SocialNetworkQueryService`의 dynamic pruning 공식 참고:
- 기존 네트워크 노드당 엣지: `5 + intimacy × 10` → 5~15개
- 새 노드 동적 추가 시 엣지: `5 + intimacy × 5` → 5~10개

친구 추천 노드가 N개 추가되면 최대 N×10개의 새 엣지가 생긴다.
KINSHIP(50) 기준 기존 네트워크 위에 15개 추천 노드 추가 시 최대 150개 신규 엣지 — 시각적 허용 범위 상한.

DunbarCircle `SYMPATHY(15)` — "활발히 교류하는 가까운 친구" 경계 — 가 도메인 의미상 자연스러운 상한.

### threshold 유지 결정

`mutualCount + labelCount >= threshold` 조건은 변경하지 않는다.
낮은 친밀도 앵커에서 추천이 거의 나오지 않는 것은 의도된 동작이다.
(소원한 관계를 통한 소개는 관련성이 낮고 프라이버시 측면에서도 부적절)

### 친구 추천 limit 결정값

| v | limit | threshold |
|---|-------|-----------|
| 0.0 | 2 | 5 |
| 0.8 | 10 | 2 |
| 1.0 | 15 | 1 |

- MIN=2: 소원한 친구를 통해서도 최소한의 추천은 제공, 친밀도 차이를 가시적으로 드러냄
- INFLECTION=10: 0.8 변곡점에서 중간 규모 유지
- MAX=15: SYMPATHY(15) 도메인 경계와 정합, 지형도 엣지 폭발 방지

---

## Objective

`SocialExpansionQueryService`의 limit 상수를 Buzz용과 친구추천용으로 분리하고,
친구 추천 limit을 `MIN=2, INFLECTION=10, MAX=15`로 조정한다.
threshold 상수는 변경하지 않는다.

---

## Out of Scope

- threshold 로직 변경
- `SocialExpansionRepositoryAdapter` Cypher 쿼리 변경
- Buzz 수신자 limit 변경 (현행 MIN=10, INFLECTION=50, MAX=150 유지)
- API 엔드포인트 및 응답 형식 변경
