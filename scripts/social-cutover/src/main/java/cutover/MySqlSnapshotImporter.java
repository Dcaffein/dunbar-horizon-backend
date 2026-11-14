package cutover;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cutover.Snapshot.Table.FRIENDSHIPS;
import static cutover.Snapshot.Table.LABELS;
import static cutover.Snapshot.Table.MEMBERS;
import static cutover.Snapshot.Table.REQUESTS;

final class MySqlSnapshotImporter {

    private static final int BATCH_SIZE = 1000;

    boolean transfer(Connection connection, Snapshot snapshot, boolean apply, PrintStream output) throws SQLException {
        connection.setReadOnly(!apply);
        connection.setTransactionIsolation(apply ? Connection.TRANSACTION_SERIALIZABLE : Connection.TRANSACTION_REPEATABLE_READ);
        connection.setAutoCommit(false);
        try {
            validateSource(connection, snapshot);
            var matchingKeys = new EnumMap<Snapshot.Table, Set<List<Object>>>(Snapshot.Table.class);
            for (Snapshot.Table table : Snapshot.Table.values()) {
                matchingKeys.put(table, compare(connection, snapshot, table));
            }
            if (snapshot.conflictCount() > 0 || !apply) {
                connection.rollback();
                report(snapshot, matchingKeys, false, output);
                return snapshot.conflictCount() == 0;
            }

            for (Snapshot.Table table : Snapshot.Table.values()) {
                insertMissing(connection, snapshot, table, matchingKeys.get(table));
            }
            for (Snapshot.Table table : Snapshot.Table.values()) {
                int verified = compare(connection, snapshot, table).size();
                if (verified != snapshot.rows(table).size()) {
                    snapshot.conflict(table.name + ": verified " + verified + " of " + snapshot.rows(table).size());
                }
            }
            if (snapshot.conflictCount() > 0) {
                connection.rollback();
                report(snapshot, matchingKeys, false, output);
                return false;
            }
            connection.commit();
            report(snapshot, matchingKeys, true, output);
            return true;
        } catch (SQLException | RuntimeException failure) {
            try {
                connection.rollback();
            } catch (SQLException rollbackFailure) {
                failure.addSuppressed(rollbackFailure);
            }
            throw failure;
        }
    }

    private void validateSource(Connection connection, Snapshot snapshot) throws SQLException {
        var accounts = new HashSet<Long>();
        try (var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT user_id FROM users")) {
            while (rows.next()) {
                accounts.add(rows.getLong("user_id"));
            }
        }

        for (var pair : snapshot.rows(FRIENDSHIPS).keySet()) {
            validateAccounts(snapshot, FRIENDSHIPS, pair, accounts, (long) pair.get(0), (long) pair.get(1));
        }
        for (var pair : snapshot.rows(REQUESTS).keySet()) {
            validateAccounts(snapshot, REQUESTS, pair, accounts, (long) pair.get(0), (long) pair.get(1));
            if (snapshot.rows(FRIENDSHIPS).containsKey(pair)) {
                snapshot.conflict(REQUESTS.name + " " + pair + ": users are already friends");
            }
        }

        var ownerNames = new HashSet<List<Object>>();
        for (var entry : snapshot.rows(LABELS).entrySet()) {
            var label = entry.getValue();
            validateAccounts(snapshot, LABELS, entry.getKey(), accounts, (long) label.get(1));
            if (!ownerNames.add(List.of(label.get(1), label.get(2)))) {
                snapshot.conflict(LABELS.name + " " + entry.getKey() + ": duplicate name for owner " + label.get(1));
            }
        }
        for (var member : snapshot.rows(MEMBERS).keySet()) {
            long memberId = (long) member.get(1);
            validateAccounts(snapshot, MEMBERS, member, accounts, memberId);
            var label = snapshot.rows(LABELS).get(List.of(member.get(0)));
            if (label == null) {
                snapshot.conflict(MEMBERS.name + " " + member + ": missing source label");
            } else if (!snapshot.rows(FRIENDSHIPS).containsKey(Snapshot.pair((long) label.get(1), memberId))) {
                snapshot.conflict(MEMBERS.name + " " + member + ": member is not a friend of owner " + label.get(1));
            }
        }
    }

    private void validateAccounts(Snapshot snapshot, Snapshot.Table table, List<Object> key, Set<Long> accounts, long... ids) {
        for (long id : ids) {
            if (!accounts.contains(id)) {
                snapshot.conflict(table.name + " " + key + ": MySQL account " + id + " is missing");
            }
        }
    }

    private Set<List<Object>> compare(Connection connection, Snapshot snapshot, Snapshot.Table table) throws SQLException {
        var matchingKeys = new HashSet<List<Object>>();
        try (var statement = connection.prepareStatement(table.selectSql())) {
            statement.setFetchSize(Integer.MIN_VALUE);
            try (var rows = statement.executeQuery()) {
                while (rows.next()) {
                    var actual = table.read(rows);
                    var key = table.key(actual);
                    var expected = snapshot.rows(table).get(key);
                    if (expected == null) {
                        snapshot.conflict(table.name + " " + key + ": target-only key");
                    } else if (!expected.equals(actual)) {
                        snapshot.conflict(table.name + " " + key + ": target row differs from source");
                    } else {
                        matchingKeys.add(table.key(expected));
                    }
                }
            }
        }
        return matchingKeys;
    }

    private void insertMissing(Connection connection, Snapshot snapshot, Snapshot.Table table, Set<List<Object>> existing)
            throws SQLException {
        try (var statement = connection.prepareStatement(table.insertSql())) {
            int pending = 0;
            for (var entry : snapshot.rows(table).entrySet()) {
                if (existing.contains(entry.getKey())) {
                    continue;
                }
                var values = entry.getValue();
                for (int index = 0; index < values.size(); index++) {
                    statement.setObject(index + 1, values.get(index));
                }
                statement.addBatch();
                if (++pending == BATCH_SIZE) {
                    statement.executeBatch();
                    pending = 0;
                }
            }
            if (pending > 0) {
                statement.executeBatch();
            }
        }
    }

    private void report(Snapshot snapshot, Map<Snapshot.Table, Set<List<Object>>> matchingKeys,
                        boolean committed, PrintStream output) {
        for (Snapshot.Table table : Snapshot.Table.values()) {
            int matched = matchingKeys.get(table).size();
            int unmatched = snapshot.rows(table).size() - matched;
            output.printf("%s source=%d matching=%d inserted=%d unmatched=%d%n",
                    table.name, snapshot.sourceCount(table), matched, committed ? unmatched : 0, committed ? 0 : unmatched);
        }
        output.printf("conflicts=%d committed=%s%n", snapshot.conflictCount(), committed);
        snapshot.conflictExamples().forEach(output::println);
        if (snapshot.conflictCount() > snapshot.conflictExamples().size()) {
            output.println("Showing the first 50 conflicts.");
        }
    }
}
