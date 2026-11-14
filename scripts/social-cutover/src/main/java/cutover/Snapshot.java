package cutover;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class Snapshot {

    private final Map<Table, Map<List<Object>, List<Object>>> rows = new EnumMap<>(Table.class);
    private final Map<Table, Integer> sourceCounts = new EnumMap<>(Table.class);
    private final List<String> conflictExamples = new ArrayList<>();
    private long conflictCount;

    Snapshot() {
        for (Table table : Table.values()) {
            rows.put(table, new LinkedHashMap<>());
            sourceCounts.put(table, 0);
        }
    }

    void seen(Table table) {
        sourceCounts.merge(table, 1, Integer::sum);
    }

    int sourceCount(Table table) {
        return sourceCounts.get(table);
    }

    void add(Table table, Object... values) {
        List<Object> row = Collections.unmodifiableList(Arrays.asList(values));
        List<Object> key = table.key(row);
        if (rows(table).putIfAbsent(key, row) != null) {
            conflict(table.name + " " + key + ": duplicate source key");
        }
    }

    Map<List<Object>, List<Object>> rows(Table table) {
        return rows.get(table);
    }

    void conflict(String message) {
        conflictCount++;
        if (conflictExamples.size() < 50) {
            conflictExamples.add(message);
        }
    }

    long conflictCount() {
        return conflictCount;
    }

    List<String> conflictExamples() {
        return List.copyOf(conflictExamples);
    }

    static List<Object> pair(long first, long second) {
        return List.of(Math.min(first, second), Math.max(first, second));
    }

    enum Table {
        FRIENDSHIPS("friendships", 2,
                id("low_user_id"), id("high_user_id"), new Column("created_at", LocalDate.class), decimal("intimacy"),
                text("low_friend_alias"), decimal("low_interest_score"), time("low_last_interacted_at"),
                flag("low_is_muted"), flag("low_is_routable"),
                text("high_friend_alias"), decimal("high_interest_score"), time("high_last_interacted_at"),
                flag("high_is_muted"), flag("high_is_routable")),
        REQUESTS("friend_requests", 2, id("low_user_id"), id("high_user_id"), id("requester_id"), id("receiver_id"),
                time("created_at"), text("status")),
        LABELS("labels", 1, text("id"), id("owner_id"), text("name")),
        MEMBERS("label_members", 2, text("label_id"), id("member_id"));

        final String name;
        private final int keySize;
        private final List<Column> columns;

        Table(String name, int keySize, Column... columns) {
            this.name = name;
            this.keySize = keySize;
            this.columns = List.of(columns);
        }

        List<Object> key(List<Object> row) {
            return row.subList(0, keySize);
        }

        List<Object> read(ResultSet result) throws SQLException {
            List<Object> values = new ArrayList<>(columns.size());
            for (Column column : columns) {
                values.add(result.getObject(column.name(), column.type()));
            }
            return values;
        }

        String selectSql() {
            return "SELECT " + columnNames() + " FROM " + name;
        }

        String insertSql() {
            return "INSERT INTO " + name + " (" + columnNames() + ") VALUES ("
                    + String.join(", ", Collections.nCopies(columns.size(), "?")) + ")";
        }

        private String columnNames() {
            return columns.stream().map(Column::name).collect(Collectors.joining(", "));
        }

        private static Column id(String name) { return new Column(name, Long.class); }
        private static Column text(String name) { return new Column(name, String.class); }
        private static Column decimal(String name) { return new Column(name, Double.class); }
        private static Column flag(String name) { return new Column(name, Boolean.class); }
        private static Column time(String name) { return new Column(name, LocalDateTime.class); }

        private record Column(String name, Class<?> type) {}
    }
}
