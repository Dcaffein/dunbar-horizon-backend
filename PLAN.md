# PLAN: Health Check Endpoint

## 목적
`docker-compose.yml`의 `dunbarhorizon-api-server` 헬스체크 조건인
`curl -f http://localhost:8080/` 을 통과시키기 위한 `GET /` 엔드포인트 추가.

## 변경 파일

### 1. 신규 생성: `HealthCheckController`
- **경로:** `src/main/java/com/example/DunbarHorizon/global/adapter/in/web/HealthCheckController.java`
- `GET /` → `200 OK`, 응답 바디 없음 (`ResponseEntity<Void>`)

### 2. 수정: `SecurityConfig`
- **경로:** `src/main/java/com/example/DunbarHorizon/global/config/seucirty/SecurityConfig.java`
- `permitAll()` 목록에 `"/"` 추가 → 인증 없이 접근 가능

## 브랜치
`ai/feat-health-check-endpoint` (from `main`)

## 승인 후 진행
