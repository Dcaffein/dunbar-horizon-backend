# Social 데이터 이관

JDK 21에서 저장소 루트의 Gradle Wrapper로 빌드한다. 생성한 실행 파일과 `lib`가 포함된 배포 디렉터리는 애플리케이션 배포와 별개로 실행할 수 있다.

```powershell
.\gradlew.bat -p scripts/social-cutover installDist
.\scripts\social-cutover\build\install\social-cutover\bin\social-cutover.bat check
.\scripts\social-cutover\build\install\social-cutover\bin\social-cutover.bat apply
```

Linux에서는 `./gradlew`와 `scripts/social-cutover/build/install/social-cutover/bin/social-cutover`를 사용한다. 실행 환경에 접속 정보를 설정한다.

| 환경 변수 | 값 |
| --- | --- |
| `CUTOVER_NEO4J_URI` | 원본 Neo4j 드라이버 URI |
| `CUTOVER_NEO4J_DATABASE` | 원본 데이터베이스 이름 |
| `CUTOVER_NEO4J_USER`, `CUTOVER_NEO4J_PASSWORD` | 원본을 읽을 계정 |
| `CUTOVER_MYSQL_URL` | 대상 스키마를 포함한 MySQL JDBC URL |
| `CUTOVER_MYSQL_USER`, `CUTOVER_MYSQL_PASSWORD` | 대상 계정 |

1. MySQL에 Flyway 마이그레이션 V8까지 적용하고 MySQL `users`에 계정이 준비되었는지 확인한다. 이관 계정은 대상 네 테이블을 읽고 입력할 수 있어야 한다.
2. 기존 앱과 스케줄러 등 Social 데이터를 변경할 수 있는 쓰기를 중지한다. 원본과 대상의 쓰기 중지는 새 앱을 기동할 때까지 유지한다.
3. `check`로 두 DB를 비교한다. 원본과 대상이 일치하는 행, 새로 입력할 행, 충돌 키를 확인한다. 충돌은 최대 50건까지 키와 사유를 출력하고 전체 수를 표시한다.
4. `apply`는 같은 검사를 다시 하고 한 MySQL 트랜잭션에서 친구 관계, 요청, 라벨, 멤버십을 순서대로 기록한다. 전체 행의 키와 값을 확인한 뒤 커밋한다. `conflicts=0 committed=true`를 확인하고 `check`로 네 테이블 모두 `unmatched=0`인지 검증한 뒤 새 앱을 기동한다.

종료 코드 `0`은 성공, `2`는 데이터 충돌, `1`은 실행 오류다. 동일한 키와 값은 재실행 시 이미 이관된 행으로 집계한다. 대상에만 있는 키, 속성 차이, 중복 요청, 없는 계정, 친구가 아닌 라벨 멤버, 누락된 `isRoutable`은 작업을 중단시킨다. 충돌을 해결한 뒤 다시 `check`를 실행한다. MySQL 아카이브는 이관 대상 테이블과 별도로 남는다. 날짜는 원본 그대로, 시간은 MySQL의 마이크로초 정밀도로 저장한다. 도구는 비교를 위해 한 번에 원본 행을 메모리에 보유한다.

Docker 기반 로컬 검증은 `gradlew -p scripts/social-cutover test`로 실행한다.
