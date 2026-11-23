package com.example.DunbarHorizon.buzz.adapter.out.persistence;

import com.example.DunbarHorizon.buzz.application.dto.info.BuzzAudienceInfo;
import com.example.DunbarHorizon.buzz.application.dto.info.BuzzCommentInfo;
import com.example.DunbarHorizon.buzz.application.dto.info.BuzzDetailInfo;
import com.example.DunbarHorizon.buzz.application.dto.info.BuzzSummaryInfo;
import com.example.DunbarHorizon.buzz.application.dto.info.BuzzThreadDetailInfo;
import com.example.DunbarHorizon.buzz.application.dto.info.BuzzThreadDetailInfo.ThreadContent;
import com.example.DunbarHorizon.buzz.application.dto.info.BuzzThreadInfo;
import com.example.DunbarHorizon.buzz.application.dto.info.BuzzUserProfileInfo;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzQueryRepository;
import com.example.DunbarHorizon.buzz.domain.ThreadVisibility;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class BuzzQueryRepositoryAdapter implements BuzzQueryRepository {

    private static final Set<Long> NO_EXCLUDED_IDS = Set.of(-1L);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    private static final String DETAIL = """
            SELECT b.id, b.creator_id, b.text, b.image_keys, b.edited_at,
                   cu.user_id AS creator_profile_id, cu.nickname AS creator_nickname,
                   cu.profile_image AS creator_profile_image,
                   t.id AS thread_id, t.starter_id, t.visibility, t.created_at AS thread_created_at,
                   su.user_id AS starter_profile_id, su.nickname AS starter_nickname,
                   su.profile_image AS starter_profile_image
            FROM buzzes b
            LEFT JOIN active_users cu ON cu.user_id = b.creator_id
            LEFT JOIN buzz_threads t ON t.buzz_id = b.id
            LEFT JOIN active_users su ON su.user_id = t.starter_id
            WHERE b.id = :buzzId
              AND b.deleted_at IS NULL
            ORDER BY t.created_at, t.id
            """;

    private static final String AUDIENCE = """
            SELECT m.user_id, m.is_read,
                   u.user_id AS profile_id, u.nickname, u.profile_image
            FROM buzz_audience_members m
            LEFT JOIN active_users u ON u.user_id = m.user_id
            WHERE m.buzz_id = :buzzId
            ORDER BY m.user_id
            """;

    private static final String THREAD_DETAIL = """
            SELECT b.id, b.creator_id,
                   cu.user_id AS creator_profile_id, cu.nickname AS creator_nickname,
                   cu.profile_image AS creator_profile_image,
                   t.id AS thread_id, t.starter_id, t.visibility, t.text AS thread_text,
                   t.image_keys AS thread_image_keys, t.created_at AS thread_created_at,
                   su.user_id AS starter_profile_id, su.nickname AS starter_nickname,
                   su.profile_image AS starter_profile_image
            FROM buzzes b
            LEFT JOIN active_users cu ON cu.user_id = b.creator_id
            LEFT JOIN buzz_threads t ON t.id = :threadId AND t.buzz_id = b.id
            LEFT JOIN active_users su ON su.user_id = t.starter_id
            WHERE b.id = :buzzId
              AND b.deleted_at IS NULL
            """;

    private static final String COMMENTS = """
            SELECT c.id, c.author_id, c.text, c.image_keys, c.created_at,
                   u.user_id AS author_profile_id, u.nickname AS author_nickname,
                   u.profile_image AS author_profile_image
            FROM buzz_comments c
            LEFT JOIN active_users u ON u.user_id = c.author_id
            WHERE c.thread_id = :threadId
            ORDER BY c.id
            """;

    private static final String RECEIVED = """
            SELECT b.id, b.creator_id, b.text, b.image_keys, b.edited_at, m.is_read,
                   cu.user_id AS creator_profile_id, cu.nickname AS creator_nickname,
                   cu.profile_image AS creator_profile_image
            FROM buzzes b
            JOIN buzz_audience_members m ON m.buzz_id = b.id AND m.user_id = :userId
            LEFT JOIN active_users cu ON cu.user_id = b.creator_id
            WHERE b.deleted_at IS NULL
              AND b.creator_id NOT IN (:excludedCreatorIds)
            ORDER BY b.created_at DESC, b.id DESC
            LIMIT :limit OFFSET :offset
            """;

    private static final String SENT = """
            SELECT b.id, b.creator_id, b.text, b.image_keys, b.edited_at, FALSE AS is_read,
                   cu.user_id AS creator_profile_id, cu.nickname AS creator_nickname,
                   cu.profile_image AS creator_profile_image
            FROM buzzes b
            LEFT JOIN active_users cu ON cu.user_id = b.creator_id
            WHERE b.creator_id = :creatorId
              AND b.deleted_at IS NULL
            ORDER BY b.created_at DESC, b.id DESC
            LIMIT :limit OFFSET :offset
            """;

    private static final String UNREAD_CREATOR_IDS = """
            SELECT DISTINCT b.creator_id
            FROM buzzes b
            JOIN buzz_audience_members m ON m.buzz_id = b.id
            WHERE m.user_id = :userId
              AND m.is_read = FALSE
              AND b.deleted_at IS NULL
              AND b.creator_id NOT IN (:excludedCreatorIds)
            """;

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public Optional<BuzzDetailInfo> findDetail(Long buzzId) {
        return jdbc.query(DETAIL, Map.of("buzzId", buzzId), rows -> {
            if (!rows.next()) {
                return Optional.empty();
            }
            Long id = rows.getLong("id");
            Long creatorId = rows.getLong("creator_id");
            BuzzUserProfileInfo creatorProfile = profile(rows, "creator_profile_id", "creator_nickname", "creator_profile_image");
            String text = rows.getString("text");
            List<String> imageKeys = imageKeys(rows, "image_keys");
            LocalDateTime editedAt = rows.getObject("edited_at", LocalDateTime.class);
            List<BuzzThreadInfo> threads = new ArrayList<>();
            do {
                long threadId = rows.getLong("thread_id");
                if (!rows.wasNull()) {
                    threads.add(new BuzzThreadInfo(
                            threadId,
                            rows.getLong("starter_id"),
                            profile(rows, "starter_profile_id", "starter_nickname", "starter_profile_image"),
                            ThreadVisibility.valueOf(rows.getString("visibility")),
                            rows.getObject("thread_created_at", LocalDateTime.class)));
                }
            } while (rows.next());
            return Optional.of(new BuzzDetailInfo(
                    id, creatorId, creatorProfile, text, imageKeys, editedAt, List.copyOf(threads)));
        });
    }

    @Override
    public List<BuzzAudienceInfo> findAudience(Long buzzId) {
        return jdbc.query(AUDIENCE, Map.of("buzzId", buzzId), (row, index) -> new BuzzAudienceInfo(
                row.getLong("user_id"),
                profile(row, "profile_id", "nickname", "profile_image"),
                row.getBoolean("is_read")));
    }

    @Override
    public Optional<BuzzThreadDetailInfo> findThreadDetail(Long buzzId, Long threadId) {
        return jdbc.query(THREAD_DETAIL, Map.of("buzzId", buzzId, "threadId", threadId),
                (row, index) -> new BuzzThreadDetailInfo(
                        row.getLong("id"),
                        row.getLong("creator_id"),
                        profile(row, "creator_profile_id", "creator_nickname", "creator_profile_image"),
                        threadContent(row))).stream().findFirst();
    }

    @Override
    public List<BuzzCommentInfo> findComments(Long threadId) {
        return jdbc.query(COMMENTS, Map.of("threadId", threadId), (row, index) -> new BuzzCommentInfo(
                row.getLong("id"),
                row.getLong("author_id"),
                profile(row, "author_profile_id", "author_nickname", "author_profile_image"),
                row.getString("text"),
                imageKeys(row, "image_keys"),
                row.getObject("created_at", LocalDateTime.class)));
    }

    @Override
    public Slice<BuzzSummaryInfo> findReceived(Long userId, Set<Long> excludedCreatorIds, Pageable pageable) {
        return slice(RECEIVED, Map.of(
                "userId", userId,
                "excludedCreatorIds", nonEmpty(excludedCreatorIds)), pageable);
    }

    @Override
    public Slice<BuzzSummaryInfo> findSent(Long creatorId, Pageable pageable) {
        return slice(SENT, Map.of("creatorId", creatorId), pageable);
    }

    @Override
    public List<Long> findUnreadCreatorIds(Long userId, Set<Long> excludedCreatorIds) {
        return jdbc.queryForList(UNREAD_CREATOR_IDS, Map.of(
                "userId", userId,
                "excludedCreatorIds", nonEmpty(excludedCreatorIds)), Long.class);
    }

    private Slice<BuzzSummaryInfo> slice(String sql, Map<String, Object> params, Pageable pageable) {
        Map<String, Object> paged = new HashMap<>(params);
        paged.put("limit", pageable.getPageSize() + 1);
        paged.put("offset", pageable.getOffset());
        List<BuzzSummaryInfo> rows = jdbc.query(sql, paged, (row, index) -> new BuzzSummaryInfo(
                row.getLong("id"),
                row.getLong("creator_id"),
                profile(row, "creator_profile_id", "creator_nickname", "creator_profile_image"),
                row.getString("text"),
                imageKeys(row, "image_keys"),
                row.getObject("edited_at", LocalDateTime.class),
                row.getBoolean("is_read")));
        boolean hasNext = rows.size() > pageable.getPageSize();
        return new SliceImpl<>(hasNext ? rows.subList(0, pageable.getPageSize()) : rows, pageable, hasNext);
    }

    private static BuzzUserProfileInfo profile(ResultSet row, String idColumn, String nicknameColumn,
                                               String imageColumn) throws SQLException {
        row.getLong(idColumn);
        if (row.wasNull()) {
            return null;
        }
        return new BuzzUserProfileInfo(row.getString(nicknameColumn), row.getString(imageColumn));
    }

    private static ThreadContent threadContent(ResultSet row) throws SQLException {
        long threadId = row.getLong("thread_id");
        if (row.wasNull()) {
            return null;
        }
        return new ThreadContent(
                threadId,
                row.getLong("starter_id"),
                profile(row, "starter_profile_id", "starter_nickname", "starter_profile_image"),
                ThreadVisibility.valueOf(row.getString("visibility")),
                row.getString("thread_text"),
                imageKeys(row, "thread_image_keys"),
                row.getObject("thread_created_at", LocalDateTime.class));
    }

    private static List<String> imageKeys(ResultSet row, String column) throws SQLException {
        try {
            return OBJECT_MAPPER.readValue(row.getString(column), STRING_LIST);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("버즈 이미지 키를 읽을 수 없습니다.", e);
        }
    }

    private static Set<Long> nonEmpty(Set<Long> ids) {
        return ids == null || ids.isEmpty() ? NO_EXCLUDED_IDS : ids;
    }
}
