package cutover;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.SessionConfig;

import java.io.PrintStream;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

public final class SocialCutover {

    public static void main(String[] args) {
        System.exit(run(args, System.getenv(), System.out, System.err));
    }

    static int run(String[] args, Map<String, String> environment, PrintStream output, PrintStream errors) {
        if (args.length != 1 || !("check".equals(args[0]) || "apply".equals(args[0]))) {
            errors.println("Usage: social-cutover check|apply");
            return 1;
        }
        try {
            var settings = new Settings(environment);
            Snapshot snapshot;
            try (var neo4j = GraphDatabase.driver(settings.neo4jUri, AuthTokens.basic(settings.neo4jUser, settings.neo4jPassword));
                 var session = neo4j.session(SessionConfig.forDatabase(settings.neo4jDatabase))) {
                snapshot = new GraphSnapshotReader().read(session);
            }
            try (var mysql = DriverManager.getConnection(settings.mysqlUrl, settings.mysqlCredentials)) {
                return new MySqlSnapshotImporter().transfer(mysql, snapshot, "apply".equals(args[0]), output) ? 0 : 2;
            }
        } catch (MissingSetting e) {
            errors.println(e.getMessage());
        } catch (SQLException e) {
            errors.printf("MySQL migration failed (SQLState %s, code %d). Verify the target before retrying.%n",
                    e.getSQLState(), e.getErrorCode());
        } catch (RuntimeException e) {
            errors.printf("Migration failed (%s). Verify the connection and schema settings.%n", e.getClass().getSimpleName());
        }
        return 1;
    }

    private static final class Settings {
        private final String neo4jUri;
        private final String neo4jUser;
        private final String neo4jPassword;
        private final String neo4jDatabase;
        private final String mysqlUrl;
        private final Properties mysqlCredentials = new Properties();

        private Settings(Map<String, String> environment) {
            neo4jUri = required(environment, "CUTOVER_NEO4J_URI");
            neo4jUser = required(environment, "CUTOVER_NEO4J_USER");
            neo4jPassword = required(environment, "CUTOVER_NEO4J_PASSWORD");
            neo4jDatabase = required(environment, "CUTOVER_NEO4J_DATABASE");
            mysqlUrl = required(environment, "CUTOVER_MYSQL_URL");
            mysqlCredentials.setProperty("user", required(environment, "CUTOVER_MYSQL_USER"));
            mysqlCredentials.setProperty("password", required(environment, "CUTOVER_MYSQL_PASSWORD"));
            mysqlCredentials.setProperty("rewriteBatchedStatements", "true");
        }

        private static String required(Map<String, String> environment, String key) {
            String value = environment.get(key);
            if (value == null || value.isBlank()) {
                throw new MissingSetting("Missing environment variable " + key);
            }
            return value;
        }
    }

    private static final class MissingSetting extends IllegalArgumentException {
        private MissingSetting(String message) {
            super(message);
        }
    }
}
