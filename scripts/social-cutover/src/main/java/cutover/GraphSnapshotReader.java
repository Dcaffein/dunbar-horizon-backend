package cutover;

import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.exceptions.value.ValueException;
import org.neo4j.driver.types.Entity;
import org.neo4j.driver.types.Node;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static cutover.Snapshot.Table.FRIENDSHIPS;
import static cutover.Snapshot.Table.LABELS;
import static cutover.Snapshot.Table.MEMBERS;
import static cutover.Snapshot.Table.REQUESTS;

final class GraphSnapshotReader {

    Snapshot read(Session session) {
        return session.executeRead(tx -> {
            var snapshot = new Snapshot();
            long legacyCount = tx.run("MATCH (f:Friendship) RETURN count(f) AS count").single().get("count").asLong();
            if (legacyCount > 0) {
                snapshot.conflict("Found " + legacyCount + " legacy Friendship nodes. Finish the graph schema migration first.");
            }
            tx.run("MATCH (start)-[edge:FRIENDS_WITH]->(end) RETURN start, edge, end")
                    .forEachRemaining(row -> readFriendship(snapshot, row));
            tx.run("""
                    MATCH (request:FriendRequest)
                    RETURN request, [(from)-[:SENT_FRIEND_REQUEST]->(request) | from] AS requesters,
                                    [(request)-[:FRIEND_REQUEST_TO]->(to) | to] AS receivers
                    """).forEachRemaining(row -> readRequest(snapshot, row));
            tx.run("""
                    MATCH (label:Label)
                    RETURN label, [(owner)-[:OWNS_LABEL]->(label) | owner] AS owners
                    """).forEachRemaining(row -> readLabel(snapshot, row));
            tx.run("MATCH (label)-[edge:ATTACHED_TO]->(member) RETURN label, edge, member")
                    .forEachRemaining(row -> readMember(snapshot, row));
            return snapshot;
        });
    }

    private void readFriendship(Snapshot snapshot, Record row) {
        var edge = row.get("edge").asRelationship();
        readRow(snapshot, FRIENDSHIPS, edge, source -> {
            long low = accountId(row.get("start").asNode());
            long high = accountId(row.get("end").asNode());
            require(low < high, "friendship direction disagrees with low/high properties");
            require(pairId(low, high).equals(source.get("id").asString(null)), "friendship ID disagrees with endpoints");
            var lowSide = recognition(source, "low");
            var highSide = recognition(source, "high");
            snapshot.add(FRIENDSHIPS, low, high, source.get("createdAt").asLocalDate(null),
                    score(source, "intimacy", 1.0),
                    lowSide.alias(), lowSide.interest(), lowSide.lastInteraction(), lowSide.muted(), lowSide.routable(),
                    highSide.alias(), highSide.interest(), highSide.lastInteraction(), highSide.muted(), highSide.routable());
        });
    }

    private void readRequest(Snapshot snapshot, Record row) {
        readRow(snapshot, REQUESTS, row.get("request").asNode(), source -> {
            long requester = singleAccount(row.get("requesters"), "requester");
            long receiver = singleAccount(row.get("receivers"), "receiver");
            require(requester != receiver, "requester and receiver are the same");
            require(pairId(requester, receiver).equals(source.get("id").asString(null)), "request ID disagrees with endpoints");
            String status = source.get("status").asString(null);
            require("PENDING".equals(status) || "HIDDEN".equals(status), "request status must be PENDING or HIDDEN");
            LocalDateTime createdAt = localDateTime(source.get("createdAt"));
            require(createdAt != null, "missing request creation time");
            snapshot.add(REQUESTS, Math.min(requester, receiver), Math.max(requester, receiver),
                    requester, receiver, createdAt, status);
        });
    }

    private void readLabel(Snapshot snapshot, Record row) {
        readRow(snapshot, LABELS, row.get("label").asNode(), source -> {
            String name = source.get("labelName").asString(null);
            require(name != null && !name.isBlank() && name.length() <= 20, "invalid label name");
            long owner = singleAccount(row.get("owners"), "label owner");
            snapshot.add(LABELS, labelId(source), owner, name);
        });
    }

    private void readMember(Snapshot snapshot, Record row) {
        readRow(snapshot, MEMBERS, row.get("edge").asRelationship(), source -> {
            Node label = row.get("label").asNode();
            require(label.hasLabel("Label"), "membership does not start at a Label");
            snapshot.add(MEMBERS, labelId(label), accountId(row.get("member").asNode()));
        });
    }

    private void readRow(Snapshot snapshot, Snapshot.Table table, Entity source, Consumer<Entity> reader) {
        snapshot.seen(table);
        try {
            reader.accept(source);
        } catch (IllegalArgumentException | ValueException e) {
            snapshot.conflict(table.name + " " + source.get("id") + " (" + source.elementId() + "): " + e.getMessage());
        }
    }

    private static Recognition recognition(Entity edge, String side) {
        String alias = edge.get(side + "FriendAlias").asString(null);
        require(alias == null || alias.length() <= 20, side + " alias exceeds 20 characters");
        Value routable = edge.get(side + "IsRoutable");
        require(!routable.isNull(), "missing " + side + "IsRoutable; resolve the publication setting before migration");
        return new Recognition(alias, score(edge, side + "InterestScore", Double.MAX_VALUE),
                localDateTime(edge.get(side + "LastInteractedAt")), edge.get(side + "IsMuted").asBoolean(false),
                routable.asBoolean());
    }

    private static double score(Entity entity, String property, double max) {
        double score = entity.get(property).asDouble(0.0);
        require(Double.isFinite(score) && score >= 0 && score <= max, "invalid " + property);
        return score == 0 ? 0.0 : score;
    }

    private static LocalDateTime localDateTime(Value value) {
        if (value.isNull()) {
            return null;
        }
        return value.asLocalDateTime().truncatedTo(ChronoUnit.MICROS);
    }

    private static long singleAccount(Value matches, String role) {
        List<Node> users = matches.asList(Value::asNode);
        require(users.size() == 1, "expected exactly one " + role + ", found " + users.size());
        return accountId(users.getFirst());
    }

    private static long accountId(Node node) {
        require(Stream.of("SocialUser", "UserReference", "InactiveSocialUser").anyMatch(node::hasLabel),
                "non-user endpoint " + node.elementId());
        long id = node.get("id").asLong(0);
        require(id > 0, "missing or invalid user ID at " + node.elementId());
        return id;
    }

    private static String labelId(Entity label) {
        String id = label.get("id").asString(null);
        require(id != null && !id.isBlank() && id.length() <= 36 && id.chars().allMatch(c -> c >= 33 && c <= 126),
                "invalid label ID");
        return id;
    }

    private static String pairId(long first, long second) {
        return Math.min(first, second) + "_" + Math.max(first, second);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    private record Recognition(String alias, double interest, LocalDateTime lastInteraction,
                               boolean muted, boolean routable) {}
}
