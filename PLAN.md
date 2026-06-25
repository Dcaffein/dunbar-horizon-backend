# PLAN: Social DTO 정리

## 브랜치

`ai/refactor-social-dto` (main 분기)

---

## STEP 1: Dead code 삭제 (3 files)

아무 곳에서도 import되지 않는 파일 삭제.

| 파일 | 경로 |
|------|------|
| `FriendProfileInfo.java` | `social/application/dto/info/` |
| `NetworkBetweenOneHopsResult.java` | `social/application/dto/result/` |
| `NetworkTwoHopSuggestionsResult.java` | `social/application/dto/result/` |

`info/` 서브 디렉터리는 파일 삭제 후 비게 되므로 함께 제거.

---

## STEP 2: `NetworkGraphResult` 해체

`record NetworkGraphResult(List<NodeGraphResult> nodes) {}` 삭제,
`List<NodeGraphResult>`로 직접 반환하도록 변경.

**수정 파일 (6개):**

| 파일 | 변경 내용 |
|------|-----------|
| `port/in/SocialNetworkQueryUseCase` | `getFriendsNetwork`, `getLabelNetwork` 반환 타입 변경 |
| `port/out/SocialNetworkRepository` | `getDefaultNetworkGraph`, `getLabelCustomNetwork` 반환 타입 변경 |
| `service/SocialNetworkQueryService` | 반환 타입 변경 |
| `adapter/out/.../SocialNetworkRepositoryAdapter` | 반환 타입 변경, `new NetworkGraphResult(nodes)` → `nodes` |
| `adapter/in/web/SocialQueryController` | `/me`, `/labels/{labelId}` 엔드포인트 반환 타입 변경 |
| `adapter/in/web/PerfNetworkController` | 반환 타입 변경 |

**삭제 파일 (1개):**
- `social/application/dto/result/NetworkGraphResult.java`

---

## STEP 3: `NetworkOneHopsByTwoHopResult` 해체

`record NetworkOneHopsByTwoHopResult(Long friendId) {}` 삭제,
`List<Long>`으로 직접 반환하도록 변경.

**수정 파일 (5개):**

| 파일 | 변경 내용 |
|------|-----------|
| `port/in/SocialNetworkQueryUseCase` | `getNetworkContactsOfTwoHop` 반환 타입 → `List<Long>` |
| `port/out/SocialNetworkRepository` | 동일 |
| `service/SocialNetworkQueryService` | 반환 타입 변경 |
| `adapter/out/.../SocialNetworkRepositoryAdapter` | `fetchAs(NetworkOneHopsByTwoHopResult.class).mappedBy(...)` → `fetchAs(Long.class).mappedBy((ts, r) -> r.get("friendId").asLong())` |
| `adapter/in/web/SocialQueryController` | `List<NetworkOneHopsByTwoHopResult>` → `List<Long>` |

**unused import 제거 (2개):**
- `SocialExpansionQueryService`
- `SocialExpansionRepository`

**삭제 파일 (1개):**
- `social/application/dto/result/NetworkOneHopsByTwoHopResult.java`

---

## STEP 4: Redis 캐시 주의

`NetworkGraphResult` 직렬화 포맷이 변경되므로 기존 캐시와 역직렬화 불일치 발생 가능.
배포 전 아래 키 패턴 flush 필요:

```
dunbar:network:default:*
dunbar:network:label:*
```

---

## API 응답 포맷 변경 (프론트엔드 확인 필요)

| 엔드포인트 | 변경 전 | 변경 후 |
|---|---|---|
| `GET /api/v1/networks/me` | `{ "nodes": [...] }` | `[...]` |
| `GET /api/v1/networks/labels/{labelId}` | `{ "nodes": [...] }` | `[...]` |
| `GET /api/v1/networks/mutual/two-hop` | `[{ "friendId": 1 }, ...]` | `[1, 2, ...]` |

---

## 요약

- 삭제 파일: **5개**
- 수정 파일: **9개**
