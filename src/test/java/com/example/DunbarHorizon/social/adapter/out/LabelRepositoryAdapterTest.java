package com.example.DunbarHorizon.social.adapter.out;

import com.example.DunbarHorizon.social.adapter.out.persistence.neo4j.LabelRepositoryAdapter;
import com.example.DunbarHorizon.social.adapter.out.persistence.neo4j.springData.SocialUserNeo4jRepository;
import com.example.DunbarHorizon.social.domain.label.Label;
import com.example.DunbarHorizon.social.domain.label.LabelTestFactory;
import com.example.DunbarHorizon.social.domain.socialUser.SocialUser;
import com.example.DunbarHorizon.social.domain.socialUser.exception.UserReferenceNotFoundException;
import com.example.DunbarHorizon.support.Neo4jRepositoryTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.core.Neo4jClient;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Neo4jRepositoryTest
@Import(LabelRepositoryAdapter.class)
class LabelRepositoryAdapterTest {

    @Autowired
    private LabelRepositoryAdapter labelRepository;

    @Autowired
    private SocialUserNeo4jRepository socialUserNeo4jRepository;

    @Autowired
    private Neo4jClient neo4jClient;

    private SocialUser owner;

    @BeforeEach
    void setUp() {
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();
        owner = socialUserNeo4jRepository.save(new SocialUser(1L, "소유자", "url1"));
        socialUserNeo4jRepository.save(new SocialUser(2L, "친구2", "url2"));
        socialUserNeo4jRepository.save(new SocialUser(3L, "친구3", "url3"));
        socialUserNeo4jRepository.save(new SocialUser(4L, "친구4", "url4"));
    }

    private Label createLabel(String name, Long... memberIds) {
        Label label = LabelTestFactory.createLabel(owner, name);
        labelRepository.create(label);
        for (Long memberId : memberIds) {
            labelRepository.addMember(label.getId(), memberId);
        }
        return label;
    }

    @Test
    @DisplayName("라벨을 생성하면 소유자와 OWNS_LABEL로 연결되고 멤버 없이 조회된다")
    void create_소유자와_연결된다() {
        // given
        Label label = LabelTestFactory.createLabel(owner, "운동 모임");

        // when
        labelRepository.create(label);

        // then
        Label found = labelRepository.findById(label.getId()).orElseThrow();
        assertThat(found.getLabelName()).isEqualTo("운동 모임");
        assertThat(found.getOwner().getId()).isEqualTo(owner.getId());
        assertThat(found.getMembers()).isEmpty();
    }

    @Test
    @DisplayName("소유자가 비활성이면 UserReferenceNotFoundException이 발생하고 라벨이 생성되지 않는다")
    void create_비활성_소유자면_예외() {
        // given
        Label label = LabelTestFactory.createLabel(owner, "운동 모임");
        deactivate(owner.getId());

        // when & then
        assertThatThrownBy(() -> labelRepository.create(label))
                .isInstanceOf(UserReferenceNotFoundException.class);
        assertThat(count("MATCH (l:Label) RETURN count(l)")).isZero();
    }

    @Test
    @DisplayName("라벨 이름을 변경하면 이름만 바뀌고 멤버는 유지된다")
    void rename_이름만_바뀐다() {
        // given
        Label label = createLabel("운동 모임", 2L);

        // when
        labelRepository.rename(label.getId(), "독서 모임");

        // then
        Label found = labelRepository.findById(label.getId()).orElseThrow();
        assertThat(found.getLabelName()).isEqualTo("독서 모임");
        assertThat(memberIdsOf(label.getId())).containsExactly(2L);
    }

    @Test
    @DisplayName("라벨을 삭제하면 라벨 노드와 관계가 사라지고 사용자는 남는다")
    void delete_라벨만_사라진다() {
        // given
        Label label = createLabel("운동 모임", 2L);

        // when
        labelRepository.delete(label.getId());

        // then
        assertThat(count("MATCH (l:Label) RETURN count(l)")).isZero();
        assertThat(count("MATCH (u:SocialUser) RETURN count(u)")).isEqualTo(4L);
    }

    @Test
    @DisplayName("같은 멤버를 두 번 추가해도 ATTACHED_TO 관계는 하나만 남는다")
    void addMember_멱등하다() {
        // given
        Label label = createLabel("운동 모임", 2L);

        // when
        labelRepository.addMember(label.getId(), 2L);

        // then
        assertThat(count("MATCH (:Label)-[r:ATTACHED_TO]->() RETURN count(r)")).isEqualTo(1L);
    }

    @Test
    @DisplayName("비활성 사용자를 멤버로 추가하면 UserReferenceNotFoundException이 발생한다")
    void addMember_비활성_사용자면_예외() {
        // given
        Label label = createLabel("운동 모임");
        deactivate(2L);

        // when & then
        assertThatThrownBy(() -> labelRepository.addMember(label.getId(), 2L))
                .isInstanceOf(UserReferenceNotFoundException.class);
        assertThat(memberIdsOf(label.getId())).isEmpty();
    }

    @Test
    @DisplayName("멤버 여부를 ID로 판단한다")
    void existsMember_ID로_판단한다() {
        // given
        Label label = createLabel("운동 모임", 2L);

        // when & then
        assertThat(labelRepository.existsMember(label.getId(), 2L)).isTrue();
        assertThat(labelRepository.existsMember(label.getId(), 3L)).isFalse();
    }

    @Test
    @DisplayName("비활성 멤버는 멤버로 판단하지 않는다")
    void existsMember_비활성_멤버는_없는_것으로_본다() {
        // given
        Label label = createLabel("운동 모임", 2L);
        deactivate(2L);

        // when & then
        assertThat(labelRepository.existsMember(label.getId(), 2L)).isFalse();
    }

    @Test
    @DisplayName("단건 멤버 제거 후 해당 ATTACHED_TO 관계만 그래프에서 사라진다")
    void removeMember_해당_관계만_사라진다() {
        // given
        Label label = createLabel("운동 모임", 2L, 3L);

        // when
        labelRepository.removeMember(label.getId(), 2L);

        // then
        assertThat(memberIdsOf(label.getId())).containsExactly(3L);
    }

    @Test
    @DisplayName("비활성 멤버를 단건 제거하려 하면 관계를 건드리지 않는다")
    void removeMember_비활성_멤버는_건드리지_않는다() {
        // given
        Label label = createLabel("운동 모임", 2L);
        deactivate(2L);

        // when
        labelRepository.removeMember(label.getId(), 2L);

        // then
        assertThat(memberIdsOf(label.getId())).containsExactly(2L);
    }

    @Test
    @DisplayName("멤버 교체 시 빠진 멤버는 지우고 새 멤버는 만들며, 유지되는 멤버의 관계는 그대로 둔다")
    void replaceMembers_델타만_반영한다() {
        // given
        Label label = createLabel("운동 모임", 2L, 3L);
        String keptRelationship = attachedElementId(label.getId(), 3L);

        // when
        labelRepository.replaceMembers(label.getId(), List.of(3L, 4L));

        // then
        assertThat(memberIdsOf(label.getId())).containsExactlyInAnyOrder(3L, 4L);
        assertThat(attachedElementId(label.getId(), 3L)).isEqualTo(keptRelationship);
    }

    @Test
    @DisplayName("빈 목록으로 교체하면 모든 멤버가 제거된다")
    void replaceMembers_빈_목록이면_전부_제거한다() {
        // given
        Label label = createLabel("운동 모임", 2L, 3L);

        // when
        labelRepository.replaceMembers(label.getId(), List.of());

        // then
        assertThat(memberIdsOf(label.getId())).isEmpty();
    }

    @Test
    @DisplayName("보이는 멤버만으로 교체해도 비활성 멤버의 관계는 남아, 재활성화하면 라벨에 다시 보인다")
    void replaceMembers_비활성_멤버의_관계는_유지한다() {
        // given
        Label label = createLabel("운동 모임", 2L, 3L);
        deactivate(2L);

        // when
        labelRepository.replaceMembers(label.getId(), List.of(3L, 4L));
        activate(2L);

        // then
        assertThat(labelRepository.findById(label.getId()).orElseThrow().getMembers())
                .extracting(member -> member.getId())
                .containsExactlyInAnyOrder(2L, 3L, 4L);
    }

    @Test
    @DisplayName("친구 끊기 정리는 소유자의 라벨에서 그 멤버만 지우고 소유 관계와 다른 멤버, 다른 사람의 라벨은 남긴다")
    void removeMemberFromOwnedLabels_대상_관계만_지운다() {
        // given
        Label labelA = createLabel("라벨A", 2L, 3L);
        Label labelB = createLabel("라벨B", 2L);
        SocialUser otherOwner = socialUserNeo4jRepository.findById(3L).orElseThrow();
        Label othersLabel = LabelTestFactory.createLabel(otherOwner, "남의 라벨");
        labelRepository.create(othersLabel);
        labelRepository.addMember(othersLabel.getId(), 2L);

        // when
        labelRepository.removeMemberFromOwnedLabels(owner.getId(), 2L);

        // then
        assertThat(memberIdsOf(labelA.getId())).containsExactly(3L);
        assertThat(memberIdsOf(labelB.getId())).isEmpty();
        assertThat(labelRepository.findAllByOwner_Id(owner.getId()))
                .extracting(Label::getId)
                .containsExactlyInAnyOrder(labelA.getId(), labelB.getId());
        assertThat(memberIdsOf(othersLabel.getId())).containsExactly(2L);
    }

    @Test
    @DisplayName("친구 끊기 정리는 비활성 멤버의 관계도 지워, 재활성화해도 라벨에 다시 보이지 않는다")
    void removeMemberFromOwnedLabels_비활성_멤버도_지운다() {
        // given
        Label label = createLabel("운동 모임", 2L);
        deactivate(2L);

        // when
        labelRepository.removeMemberFromOwnedLabels(owner.getId(), 2L);
        activate(2L);

        // then
        assertThat(memberIdsOf(label.getId())).isEmpty();
    }

    private void deactivate(Long userId) {
        neo4jClient.query("MATCH (u:SocialUser {id: $id}) REMOVE u:UserReference SET u:InactiveSocialUser")
                .bind(userId).to("id")
                .run();
    }

    private void activate(Long userId) {
        neo4jClient.query("MATCH (u:SocialUser {id: $id}) REMOVE u:InactiveSocialUser SET u:UserReference")
                .bind(userId).to("id")
                .run();
    }

    private Set<Long> memberIdsOf(String labelId) {
        return new HashSet<>(neo4jClient.query("MATCH (:Label {id: $labelId})-[:ATTACHED_TO]->(m) RETURN m.id")
                .bind(labelId).to("labelId")
                .fetchAs(Long.class)
                .all());
    }

    private String attachedElementId(String labelId, Long memberId) {
        return neo4jClient.query("MATCH (:Label {id: $labelId})-[r:ATTACHED_TO]->({id: $memberId}) RETURN elementId(r)")
                .bind(labelId).to("labelId")
                .bind(memberId).to("memberId")
                .fetchAs(String.class)
                .one()
                .orElseThrow();
    }

    private Long count(String cypher) {
        return neo4jClient.query(cypher).fetchAs(Long.class).one().orElseThrow();
    }
}
