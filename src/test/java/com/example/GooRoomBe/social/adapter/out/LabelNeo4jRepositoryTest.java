package com.example.GooRoomBe.social.adapter.out;

import com.example.GooRoomBe.social.adapter.out.neo4j.springData.LabelNeo4jRepository;
import com.example.GooRoomBe.social.adapter.out.neo4j.springData.SocialUserNeo4jRepository;
import com.example.GooRoomBe.social.domain.label.Label;
import com.example.GooRoomBe.social.domain.label.LabelTestFactory;
import com.example.GooRoomBe.social.domain.socialUser.SocialUser;
import com.example.GooRoomBe.support.Neo4jRepositoryTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Neo4jRepositoryTest
class LabelNeo4jRepositoryTest {

    @Autowired
    private LabelNeo4jRepository labelRepository;

    @Autowired
    private SocialUserNeo4jRepository socialUserNeo4jRepository;

    private SocialUser owner;
    private SocialUser friend1;
    private SocialUser friend2;

    @BeforeEach
    void setUp() {
        owner = socialUserNeo4jRepository.save(new SocialUser(1L, "소유자", "url1"));
        friend1 = socialUserNeo4jRepository.save(new SocialUser(2L, "친구1", "url2"));
        friend2 = socialUserNeo4jRepository.save(new SocialUser(3L, "친구2", "url3"));
    }

    @Test
    @DisplayName("소유자 ID와 라벨 이름으로 존재 여부를 정확히 판단한다")
    void existsByOwner_IdAndLabelName_Success() {
        // given
        Label label = LabelTestFactory.createLabel(owner, "운동 모임", true);
        labelRepository.save(label);

        // when
        boolean exists = labelRepository.existsByOwner_IdAndLabelName(owner.getId(), "운동 모임");
        boolean notExists = labelRepository.existsByOwner_IdAndLabelName(owner.getId(), "독서 모임");

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("특정 소유자의 모든 라벨 목록을 조회한다")
    void findAllByOwner_Id_Success() {
        // given
        labelRepository.save(LabelTestFactory.createLabel(owner, "라벨A", true));
        labelRepository.save(LabelTestFactory.createLabel(owner, "라벨B", true));

        // when
        List<Label> labels = labelRepository.findAllByOwner_Id(owner.getId());

        // then
        assertThat(labels).hasSize(2);
        assertThat(labels).extracting(Label::getLabelName)
                .containsExactlyInAnyOrder("라벨A", "라벨B");
    }

    @Test
    @DisplayName("멤버 교체 시 그래프 상의 HAS_MEMBER 관계가 갱신되어야 한다")
    void updateMembers_Graph_Consistency() {
        // given
        Label label = LabelTestFactory.createLabel(owner, "프로젝트", true);
        LabelTestFactory.addMember(label, friend1);
        labelRepository.save(label);

        // when
        Label savedLabel = labelRepository.findAllByOwner_Id(owner.getId()).get(0);
        LabelTestFactory.updateMembers(savedLabel, Set.of(friend2));
        labelRepository.save(savedLabel);

        // then
        Label retrieved = labelRepository.findById(savedLabel.getId()).get();
        assertThat(retrieved.getMembers()).hasSize(1);
        assertThat(retrieved.getMembers().iterator().next().getId()).isEqualTo(friend2.getId());
    }
}