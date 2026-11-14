package cutover;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.GraphDatabase;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class SocialCutoverTest {

    @Container
    private static final Neo4jContainer<?> GRAPH = new Neo4jContainer<>("neo4j:5.24")
            .withAdminPassword("cutover-test");

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0").withCommand("--skip-log-bin");

    @BeforeAll
    static void migrateSchema() {
        Flyway.configure().dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("filesystem:" + System.getProperty("cutover.migrations"))
                .target("8").load().migrate();
    }

    @BeforeEach
    void seed() throws SQLException {
        graph("MATCH (node) DETACH DELETE node");
        mysql("DROP TRIGGER IF EXISTS change_imported_label",
                "DELETE FROM label_members", "DELETE FROM labels", "DELETE FROM friend_requests",
                "DELETE FROM friendships", "DELETE FROM users", "DELETE FROM archived_friendships");
        mysql("""
                INSERT INTO users (user_id, email, nickname, role, status) VALUES
                    (1, 'one@test.invalid', 'one', 'USER', 'ACTIVE'),
                    (2, 'two@test.invalid', 'two', 'USER', 'DORMANT'),
                    (3, 'three@test.invalid', 'three', 'USER', 'ACTIVE'),
                    (4, 'four@test.invalid', 'four', 'USER', 'ACTIVE')
                """);
        graph("""
                CREATE (one:SocialUser:UserReference {id: 1}), (two:SocialUser:InactiveSocialUser {id: 2}),
                       (three:SocialUser:UserReference {id: 3}), (four:SocialUser:UserReference {id: 4}),
                       (one)-[:FRIENDS_WITH {id: '1_2', createdAt: date('2026-09-15'), intimacy: 0.63,
                           lowFriendAlias: '별명', lowInterestScore: 50.5,
                           lowLastInteractedAt: localdatetime('2026-10-24T05:03:00.123456789'),
                           lowIsMuted: true, lowIsRoutable: false,
                           highFriendAlias: 'other', highInterestScore: 12.25,
                           highLastInteractedAt: localdatetime('2026-11-02T15:30:25.987654321'),
                           highIsMuted: false, highIsRoutable: true}]->(two),
                       (four)-[:SENT_FRIEND_REQUEST]->(req:FriendRequest {id: '3_4', status: 'HIDDEN',
                           createdAt: localdatetime('2026-11-13T18:30:00.999999999')}),
                       (req)-[:FRIEND_REQUEST_TO]->(three),
                       (two)-[:OWNS_LABEL]->(label:Label {id: 'friends-2', labelName: '산책'}),
                       (label)-[:ATTACHED_TO]->(one)
                """);
    }

    @Test
    @DisplayName("미리보기 후 방향별 속성을 이관하고 재실행하면 동일한 행만 확인한다")
    void apply_PreservesValuesAndIsRepeatable() throws SQLException {
        // given & when
        mysql("""
                INSERT INTO archived_friendships (id, archived_at, friended_at, useraid, userbid)
                VALUES ('existing-archive', '2026-09-18 12:05:00', '2026-09-15', 1, 3)
                """);
        Run check = run("check");
        assertEquals(0, check.code(), check.output());
        assertTrue(check.output().contains("friendships source=1 matching=0 inserted=0 unmatched=1"), check.output());
        assertEquals(0, count("friendships"));

        Run apply = run("apply");

        // then
        assertEquals(0, apply.code(), apply.output());
        assertTrue(apply.output().contains("conflicts=0 committed=true"), apply.output());
        assertEquals(List.of(1L, 2L, LocalDate.parse("2026-09-15"), 0.63,
                        "별명", 50.5, LocalDateTime.parse("2026-10-24T05:03:00.123456"), true, false,
                        "other", 12.25, LocalDateTime.parse("2026-11-02T15:30:25.987654"), false, true),
                row(Snapshot.Table.FRIENDSHIPS));
        assertEquals(List.of(3L, 4L, 4L, 3L, LocalDateTime.parse("2026-11-13T18:30:00.999999"), "HIDDEN"),
                row(Snapshot.Table.REQUESTS));
        assertEquals(List.of("friends-2", 2L, "산책"), row(Snapshot.Table.LABELS));
        assertEquals(List.of("friends-2", 1L), row(Snapshot.Table.MEMBERS));
        Run repeated = run("apply");
        assertEquals(0, repeated.code(), repeated.output());
        for (Snapshot.Table table : Snapshot.Table.values()) {
            assertTrue(repeated.output().contains(table.name + " source=1 matching=1 inserted=0 unmatched=0"),
                    repeated.output());
            assertEquals(1, count(table.name));
        }
        assertEquals(1, count("archived_friendships"));
    }

    @Test
    @DisplayName("중복 요청과 계정 누락, 친구가 아닌 라벨 멤버를 보고하고 쓰지 않는다")
    void apply_RejectsSourceConflicts() throws SQLException {
        // given
        graph("""
                MATCH (three:SocialUser {id: 3}), (four:SocialUser {id: 4}), (label:Label {id: 'friends-2'})
                CREATE (three)-[:SENT_FRIEND_REQUEST]->(request:FriendRequest {id: '3_4', status: 'PENDING',
                           createdAt: localdatetime('2026-11-13T00:00:00')}), (request)-[:FRIEND_REQUEST_TO]->(four),
                       (label)-[:ATTACHED_TO]->(three),
                       (missing:SocialUser {id: 99})-[:OWNS_LABEL]->(:Label {id: 'missing-owner', labelName: 'missing'})
                """);

        // when
        Run result = run("apply");

        // then
        assertEquals(2, result.code(), result.output());
        assertTrue(result.output().contains("friend_requests [3, 4]: duplicate source key"), result.output());
        assertTrue(result.output().contains("labels [missing-owner]: MySQL account 99 is missing"), result.output());
        assertTrue(result.output().contains("label_members [friends-2, 3]: member is not a friend of owner 2"), result.output());
        assertEquals(0, count("friendships"));
    }

    @Test
    @DisplayName("대상의 기존 값이나 대상에만 존재하는 행과 충돌하면 보존한다")
    void apply_PreservesExistingRows_OnTargetConflict() throws SQLException {
        // given
        mysql("INSERT INTO labels VALUES ('friends-2', 2, 'different'), ('extra', 1, 'extra')");

        // when
        Run result = run("apply");

        // then
        assertEquals(2, result.code(), result.output());
        assertTrue(result.output().contains("labels [friends-2]: target row differs from source"), result.output());
        assertTrue(result.output().contains("labels [extra]: target-only key"), result.output());
        assertEquals(0, count("friendships"));
        assertEquals(2, count("labels"));
    }

    @Test
    @DisplayName("쓰기 이후 행 검증이 실패하면 먼저 입력한 테이블까지 롤백한다")
    void apply_RollsBackAllWrites_OnVerificationFailure() throws SQLException {
        // given
        mysql("CREATE TRIGGER change_imported_label BEFORE INSERT ON labels FOR EACH ROW SET NEW.name = 'changed'");

        // when
        Run result = run("apply");

        // then
        assertEquals(2, result.code(), result.output());
        assertTrue(result.output().contains("labels [friends-2]: target row differs from source"), result.output());
        assertTrue(result.output().contains("committed=false"), result.output());
        for (Snapshot.Table table : Snapshot.Table.values()) {
            assertEquals(0, count(table.name));
        }
    }

    @Test
    @DisplayName("공개 설정 누락은 공개 기본값으로 변환하지 않고 이관을 중단한다")
    void apply_RejectsMissingRoutability() throws SQLException {
        // given
        graph("MATCH ()-[edge:FRIENDS_WITH]->() REMOVE edge.highIsRoutable");

        // when & then
        Run result = run("apply");
        assertEquals(2, result.code(), result.output());
        assertTrue(result.output().contains("missing highIsRoutable"), result.output());
        assertEquals(0, count("friendships"));
    }

    private static Run run(String mode) {
        var output = new ByteArrayOutputStream();
        try (var stream = new PrintStream(output, true, UTF_8)) {
            int code = SocialCutover.run(new String[]{mode}, Map.of(
                    "CUTOVER_NEO4J_URI", GRAPH.getBoltUrl(), "CUTOVER_NEO4J_USER", "neo4j",
                    "CUTOVER_NEO4J_PASSWORD", GRAPH.getAdminPassword(), "CUTOVER_NEO4J_DATABASE", "neo4j",
                    "CUTOVER_MYSQL_URL", MYSQL.getJdbcUrl(), "CUTOVER_MYSQL_USER", MYSQL.getUsername(),
                    "CUTOVER_MYSQL_PASSWORD", MYSQL.getPassword()), stream, stream);
            return new Run(code, output.toString(UTF_8));
        }
    }

    private static void graph(String cypher) {
        try (var driver = GraphDatabase.driver(GRAPH.getBoltUrl(), AuthTokens.basic("neo4j", GRAPH.getAdminPassword()));
             var session = driver.session()) {
            session.run(cypher).consume();
        }
    }

    private static void mysql(String... commands) throws SQLException {
        try (var connection = DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             var statement = connection.createStatement()) {
            for (String command : commands) {
                statement.executeUpdate(command);
            }
        }
    }

    private static int count(String table) throws SQLException {
        try (var connection = DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rows.next();
            return rows.getInt(1);
        }
    }

    private static List<Object> row(Snapshot.Table table) throws SQLException {
        try (var connection = DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             var statement = connection.createStatement(); var rows = statement.executeQuery(table.selectSql())) {
            var values = new ArrayList<List<Object>>();
            while (rows.next()) {
                values.add(table.read(rows));
            }
            assertEquals(1, values.size(), table.name);
            return values.getFirst();
        }
    }

    private record Run(int code, String output) {}
}
