# Task 71: 개발용 Flag 시드 API

## 배경

프론트엔드가 Flag의 다양한 상태(모집 중, 종료, 후기 있음 등)를 UI/UX 테스트하려면
실제 DB에 해당 상태의 Flag가 존재해야 한다.
그런데 정상 플로우(Flag 생성 → 참여 → 자연 종료)를 거치면 시간이 걸리고,
`CLOSED` 상태(과거 endDateTime) + 후기가 동시에 존재하는 조합은 만들 수 없다.

## 목표

`POST /api/dev/flags/seed` 단일 호출로 아래 조합을 한 번에 심는다.

- `OPEN`: 미래 일정을 가진 모집 중 Flag + 참여자
- `CLOSED`: 과거 일정을 가진 종료된 Flag + 참여자 + 후기

응답은 생성된 Flag ID 목록을 반환한다.

## 제약

| 항목 | 내용 |
|------|------|
| 프로파일 | `@Profile("local")` — prod 빌드에 포함되지 않음 |
| 보안 | `/api/dev/**`는 SecurityConfig에서 이미 permitAll 처리됨 |
| 도메인 우회 | `FlagParticipant`, `FlagMemorial` 생성자가 package-private이라 JPA로 생성 불가 → JdbcTemplate 직접 INSERT |

## 설계 결정

### 상태 표현

`status` 필드는 도메인 내부에 저장되지 않는다 (계산값).
상태 기계를 우회하는 방법은 `endDateTime`을 과거로 설정하는 것.

| 요청 status | 처리 방식 |
|------------|---------|
| `OPEN`     | schedule 그대로 사용 |
| `CLOSED`   | schedule 그대로 사용 (endDateTime이 과거이면 자동으로 ENDED 계산됨) |

`deadline`이 없으면 `startDateTime - 1분`으로 기본값 처리 (FlagSchedule 내부와 동일).

### Flag INSERT (JdbcTemplate)

`flags` 테이블에 직접 INSERT.
`group_id`는 `BINARY(16)` → `UNHEX(REPLACE(uuid, '-', ''))` SQL 함수로 삽입.

### 참여자 INSERT (JdbcTemplate)

`flag_participant` 테이블에 직접 INSERT.
`can_invite = false` 기본값.

### 후기 INSERT (JdbcTemplate)

`flag_memorial` 테이블에 직접 INSERT.

## 요청 / 응답

```
POST /api/dev/flags/seed

{
  "hostUserId": 4,
  "flags": [
    {
      "title": "한강 치맥 파티",
      "status": "OPEN",
      "schedule": {
        "startDateTime": "2026-06-21T17:00:00",
        "endDateTime": "2026-06-21T21:00:00",
        "deadline": "2026-06-20T23:59:00"
      },
      "capacity": 12,
      "participantUserIds": [2, 3]
    },
    {
      "title": "5월 클라이밍 모임",
      "status": "CLOSED",
      "schedule": {
        "startDateTime": "2026-05-10T10:00:00",
        "endDateTime": "2026-05-10T13:00:00"
      },
      "capacity": 8,
      "participantUserIds": [2, 3],
      "memorials": [
        { "writerUserId": 2, "content": "정말 즐거웠어요!" },
        { "writerUserId": 3, "content": "초보자도 잘 따라갈 수 있었어요." }
      ]
    }
  ]
}

응답: { "flagIds": [1, 2] }
```

## 변경 파일 목록

| 파일 | 변경 유형 |
|------|---------|
| `flag/adapter/in/web/FlagSeedController.java` | 신규 |
| `flag/application/service/flag/FlagSeedService.java` | 신규 |

기존 `DevController` 패턴(`@Profile("local")`, `/api/dev/**`)을 그대로 따른다.

## Result

(미완료)
