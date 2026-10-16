package com.example.DunbarHorizon.social.adapter.out;

import com.example.DunbarHorizon.social.adapter.out.persistence.neo4j.LabelRepositoryAdapter;
import com.example.DunbarHorizon.social.adapter.out.persistence.neo4j.springData.SocialUserNeo4jRepository;
import com.example.DunbarHorizon.social.domain.label.Label;
import com.example.DunbarHorizon.social.domain.label.LabelTestFactory;
import com.example.DunbarHorizon.social.domain.socialUser.SocialUser;
import com.example.DunbarHorizon.support.Neo4jRepositoryTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.core.Neo4jClient;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 라벨과 그 소유자·멤버를 읽어 둔 뒤 SocialUser 노드가 바뀌어도, 라벨 쓰기가 그 노드를 덮어쓰지 않는지 검증한다.
 */
@Neo4jRepositoryTest
@Import(LabelRepositoryAdapter.class)
class LabelCascadeRegressionTest {

    private static final LocalDateTime UPDATED_AT = LocalDateTime.of(2026, 10, 14, 14, 55, 5);

    @Autowired
    private LabelRepositoryAdapter labelRepository;

    @Autowired
    private SocialUserNeo4jRepository socialUserNeo4jRepository;

    @Autowired
    private Neo4jClient neo4jClient;

    private Label staleLabel;

    @BeforeEach
    void setUp() {
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();
        SocialUser owner = socialUserNeo4jRepository.save(new SocialUser(1L, "소유자", "ownerImage"));
        socialUserNeo4jRepository.save(new SocialUser(2L, "사용자B", "oldImage"));
        socialUserNeo4jRepository.save(new SocialUser(3L, "사용자C", "imageC"));

        Label label = LabelTestFactory.createLabel(owner, "운동 모임");
        labelRepository.create(label);
        labelRepository.addMember(label.getId(), 2L);
        staleLabel = labelRepository.findById(label.getId()).orElseThrow();
    }

    @Test
    @DisplayName("라벨을 읽은 뒤 비활성화된 멤버는 라벨 쓰기 후에도 InactiveSocialUser로 남는다")
    void 비활성화_후_라벨을_써도_라벨이_되살아나지_않는다() {
        // given
        neo4jClient.query("MATCH (u:SocialUser {id: 2}) REMOVE u:UserReference SET u:InactiveSocialUser").run();

        // when
        writeEveryLabelCommand();

        // then
        assertThat(labelsOf(2L)).containsExactlyInAnyOrder("SocialUser", "InactiveSocialUser");
    }

    @Test
    @DisplayName("라벨을 읽은 뒤 갱신된 멤버·소유자 프로필은 라벨 쓰기 후에도 되돌아가지 않는다")
    void 프로필_갱신_후_라벨을_써도_속성이_되돌아가지_않는다() {
        // given
        neo4jClient.query("MATCH (u:SocialUser) WHERE u.id IN [1, 2] " +
                        "SET u.nickname = '새닉네임' + u.id, u.profileImageUrl = 'newImage', u.updatedAt = $updatedAt")
                .bind(UPDATED_AT).to("updatedAt")
                .run();

        // when
        writeEveryLabelCommand();

        // then
        for (Long userId : List.of(1L, 2L)) {
            SocialUser user = socialUserNeo4jRepository.findById(userId).orElseThrow();
            assertThat(user.getNickname()).isEqualTo("새닉네임" + userId);
            assertThat(user.getProfileImageUrl()).isEqualTo("newImage");
            assertThat(user.getUpdatedAt()).isEqualTo(UPDATED_AT);
            assertThat(labelsOf(userId)).containsExactlyInAnyOrder("SocialUser", "UserReference");
        }
    }

    /**
     * 읽어 둔 라벨과 그 소유자 스냅샷으로 라벨 쓰기 커맨드를 전부 수행한다.
     */
    private void writeEveryLabelCommand() {
        String labelId = staleLabel.getId();
        labelRepository.create(LabelTestFactory.createLabel(staleLabel.getOwner(), "새 라벨"));
        labelRepository.rename(labelId, "독서 모임");
        labelRepository.addMember(labelId, 3L);
        labelRepository.removeMember(labelId, 3L);
        labelRepository.replaceMembers(labelId, List.of(2L, 3L));
        labelRepository.removeMemberFromOwnedLabels(1L, 3L);
        labelRepository.delete(labelId);
    }

    @SuppressWarnings("unchecked")
    private Collection<String> labelsOf(Long userId) {
        return (Collection<String>) neo4jClient.query("MATCH (u:SocialUser {id: $id}) RETURN labels(u) AS labels")
                .bind(userId).to("id")
                .fetch().one().orElseThrow()
                .get("labels");
    }
}
