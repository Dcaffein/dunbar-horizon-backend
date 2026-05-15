# PLAN: Task-26 — 이메일 인증 토큰 저장소 MySQL → Redis 교체

## [도메인 수정 승인 요청]
- `EmailVerificationTokenRepository` 포트 인터페이스 메서드 전면 교체
- `EmailVerificationToken` JPA 엔티티 제거
