# Task-78: 특정 유저의 최근 Flag 5개 조회 API 추가

## 배경 및 목적

유저 프로필 방문 시 해당 유저가 호스팅하거나 참여한 Flag 중 가장 최근 5개를 조회하는 API가 필요하다.

## 의도

**엔드포인트:** `GET /api/v1/flags/users/{userId}/recent`

**정렬 기준:** `Flag.createdAt DESC`
- `startDateTime`은 미래 일정이 잡힌 Flag가 "최근"처럼 보이는 왜곡이 생김
- `createdAt`은 유저가 Flag를 생성하거나 참여 신청한 실제 활동 시점을 반영

**범위:** 호스팅 + 참여 Flag 합산, soft-delete 제외, 상위 5개

## Out of Scope

- 페이지네이션 (고정 5개)
- 역할(HOST/PARTICIPANT) 필터링
