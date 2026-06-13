# PLAN — Task 71: 개발용 Flag 시드 API

## 작업 목표

`POST /api/dev/flags/seed`로 임의 상태의 Flag (+ 참여자, 후기)를 한 번에 삽입한다.
`FlagParticipant` / `FlagMemorial` 생성자가 package-private이라 JPA 우회 → JdbcTemplate 직접 INSERT.

---

## 현황 분석

### 기존 DevController 패턴

```
account/adapter/in/web/DevController.java     — @Profile("local"), /api/dev
account/application/service/DevUserService.java — @Profile("local"), @Transactional
```

### 테이블 · 컬럼 매핑

**`flags`** (`@Table(name = "flags")`)

| 컬럼 | 출처 |
|------|------|
| `host_id` | request.hostUserId |
| `title` | flagItem.title |
| `description` | null (dev 용도) |
| `capacity` | flagItem.capacity |
| `deadline` | schedule.deadline (없으면 startDateTime - 1분) |
| `start_date_time` | schedule.startDateTime |
| `end_date_time` | schedule.endDateTime |
| `group_id` | `UNHEX(REPLACE(UUID, '-', ''))` — BINARY(16) |
| `parent_id` | null |
| `is_preserved` | false |
| `created_at`, `updated_at` | NOW() |

**`flag_participant`** (entity 기본 네이밍 → `flag_participant`)

| 컬럼 | 값 |
|------|-----|
| `flag_id` | 삽입된 flag ID |
| `participant_id` | participantUserIds 각 원소 |
| `can_invite` | false |
| `created_at`, `updated_at` | NOW() |

**`flag_memorial`** (entity 기본 네이밍 → `flag_memorial`)

| 컬럼 | 값 |
|------|-----|
| `flag_id` | 삽입된 flag ID |
| `writer_id` | memorial.writerUserId |
| `content` | memorial.content |
| `created_at`, `updated_at` | NOW() |

---

## 구현 계획

### 1. `FlagSeedController` (신규)

위치: `flag/adapter/in/web/FlagSeedController.java`

```java
@Profile("local")
@RestController
@RequestMapping("/api/dev/flags")
@RequiredArgsConstructor
public class FlagSeedController {

    private final FlagSeedService flagSeedService;

    @PostMapping("/seed")
    public ResponseEntity<FlagSeedResponse> seed(@RequestBody FlagSeedRequest request) {
        List<Long> ids = flagSeedService.seed(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new FlagSeedResponse(ids));
    }
}
```

요청 DTO:
```java
record FlagSeedRequest(Long hostUserId, List<FlagSeedItem> flags) {}

record FlagSeedItem(
    String title,
    String status,               // "OPEN" | "CLOSED"
    ScheduleSeedItem schedule,
    Integer capacity,
    List<Long> participantUserIds,
    List<MemorialSeedItem> memorials  // nullable
) {}

record ScheduleSeedItem(
    LocalDateTime startDateTime,
    LocalDateTime endDateTime,
    LocalDateTime deadline        // nullable — 없으면 startDateTime - 1분
) {}

record MemorialSeedItem(Long writerUserId, String content) {}

record FlagSeedResponse(List<Long> flagIds) {}
```

### 2. `FlagSeedService` (신규)

위치: `flag/application/service/flag/FlagSeedService.java`

```java
@Profile("local")
@Service
@RequiredArgsConstructor
@Transactional
public class FlagSeedService {

    private final JdbcTemplate jdbc;

    public List<Long> seed(FlagSeedRequest request) {
        List<Long> ids = new ArrayList<>();
        for (FlagSeedItem item : request.flags()) {
            Long flagId = insertFlag(request.hostUserId(), item);
            insertParticipants(flagId, item.participantUserIds());
            if (item.memorials() != null) {
                insertMemorials(flagId, item.memorials());
            }
            ids.add(flagId);
        }
        return ids;
    }

    private Long insertFlag(Long hostId, FlagSeedItem item) {
        String sql = """
            INSERT INTO flags
              (host_id, title, description, capacity,
               deadline, start_date_time, end_date_time,
               group_id, parent_id, is_preserved, created_at, updated_at)
            VALUES (?, ?, NULL, ?, ?, ?, ?, UNHEX(REPLACE(?, '-', '')), NULL, false, NOW(), NOW())
            """;

        LocalDateTime deadline = item.schedule().deadline() != null
                ? item.schedule().deadline()
                : item.schedule().startDateTime().minusMinutes(1);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, hostId);
            ps.setString(2, item.title());
            ps.setInt(3, item.capacity());
            ps.setObject(4, deadline);
            ps.setObject(5, item.schedule().startDateTime());
            ps.setObject(6, item.schedule().endDateTime());
            ps.setString(7, UUID.randomUUID().toString());
            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    private void insertParticipants(Long flagId, List<Long> participantIds) {
        String sql = """
            INSERT INTO flag_participant (flag_id, participant_id, can_invite, created_at, updated_at)
            VALUES (?, ?, false, NOW(), NOW())
            """;
        for (Long pid : participantIds) {
            jdbc.update(sql, flagId, pid);
        }
    }

    private void insertMemorials(Long flagId, List<MemorialSeedItem> memorials) {
        String sql = """
            INSERT INTO flag_memorial (flag_id, writer_id, content, created_at, updated_at)
            VALUES (?, ?, ?, NOW(), NOW())
            """;
        for (MemorialSeedItem m : memorials) {
            jdbc.update(sql, flagId, m.writerUserId(), m.content());
        }
    }
}
```

---

## 테스트 계획

Mockito 단위 테스트 대신 수동 확인 (JdbcTemplate + DB 직접 조회).
`./gradlew bootRun` 후 curl 또는 Swagger로 검증.

---

## 변경 파일 요약

| 파일 | 유형 |
|------|------|
| `flag/adapter/in/web/FlagSeedController.java` | 신규 |
| `flag/application/service/flag/FlagSeedService.java` | 신규 |
