package com.example.DunbarHorizon.social.adapter.out.persistence;

import com.example.DunbarHorizon.account.domain.User;
import com.example.DunbarHorizon.buzz.adapter.out.social.LabelRecipientStrategyAdapter;
import com.example.DunbarHorizon.buzz.application.dto.info.recipient.LabelRecipientSpec;
import com.example.DunbarHorizon.social.adapter.out.persistence.jdbc.LabelJdbcRepository;
import com.example.DunbarHorizon.social.application.port.out.LabelRepository;
import com.example.DunbarHorizon.social.adapter.out.persistence.jdbc.FriendshipJdbcRepository;
import com.example.DunbarHorizon.social.adapter.out.persistence.jdbc.UserProfileJdbcRepository;
import com.example.DunbarHorizon.social.application.dto.result.LabelMemberResult;
import com.example.DunbarHorizon.social.application.dto.result.LabelResult;
import com.example.DunbarHorizon.social.application.service.LabelService;
import com.example.DunbarHorizon.social.domain.friend.FriendRecognition;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendshipNotFoundException;
import com.example.DunbarHorizon.social.domain.label.Label;
import com.example.DunbarHorizon.social.domain.label.LabelNamer;
import com.example.DunbarHorizon.social.domain.label.LabelMemberRegistrar;
import com.example.DunbarHorizon.social.domain.label.exception.DuplicateLabelMemberException;
import com.example.DunbarHorizon.social.domain.label.exception.DuplicateLabelNameException;
import com.example.DunbarHorizon.social.domain.label.exception.InvalidLabelNameException;
import com.example.DunbarHorizon.social.domain.label.exception.LabelAuthorizationException;
import com.example.DunbarHorizon.social.domain.label.exception.LabelNotFoundException;
import com.example.DunbarHorizon.social.domain.label.exception.NonFriendLabelMemberException;
import com.example.DunbarHorizon.social.domain.socialUser.exception.UserReferenceNotFoundException;
import com.example.DunbarHorizon.support.JpaRepositoryTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JpaRepositoryTest
@Import({LabelJdbcRepository.class, FriendshipJdbcRepository.class, FriendshipPersistenceAdapter.class, LabelPersistenceAdapter.class, UserProfileJdbcRepository.class,
        LabelService.class, LabelNamer.class, LabelMemberRegistrar.class})
class LabelPersistenceAdapterTest {

    @Autowired private TestEntityManager em;
    @Autowired private LabelJdbcRepository labelJdbcRepository;
    @Autowired private LabelRepository labelRepository;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private FriendshipPersistenceAdapter friendships;
    @Autowired private LabelService service;

    private User owner;
    private User first;
    private User second;

    @BeforeEach
    void setUp() {
        owner = createUser("owner");
        first = createUser("first");
        second = createUser("second");
    }

    @Test
    @DisplayName("라벨을 생성하고 이름을 바꾸면 기존 응답으로 조회된다")
    void getLabel_ReturnsExistingResponse_AfterRename() {
        // given
        Label label = service.createLabel(owner.getId(), "친구");
        connect(owner, first);
        service.addMemberToLabel(owner.getId(), label.getId(), first.getId());

        // when
        service.updateLabel(label.getId(), owner.getId(), "함께 운동");

        // then
        var expected = new LabelResult(label.getId(), "함께 운동", 1);
        assertThat(service.getLabelById(owner.getId(), label.getId())).isEqualTo(expected);
        assertThat(service.getAllLabels(owner.getId())).containsExactly(expected);
        assertThat(service.getLabelsByMember(owner.getId(), first.getId())).containsExactly(expected);
        assertThat(service.getLabelMembers(owner.getId(), label.getId()))
                .containsExactly(new LabelMemberResult(first.getId(), "first"));
        em.clear();
        assertThatThrownBy(() -> labelRepository.findById(label.getId()).orElseThrow().getMemberIds().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("이름 유일성은 사용자별로 적용하고 대소문자가 다른 이름은 구분한다")
    void changeName_EnforcesUniqueness_PerOwner() {
        // given
        Label firstLabel = service.createLabel(owner.getId(), "Work");
        Label secondLabel = service.createLabel(owner.getId(), "work");
        service.createLabel(first.getId(), "Work");

        service.updateLabel(firstLabel.getId(), owner.getId(), "Work");

        // when & then
        assertThatThrownBy(() -> service.updateLabel(secondLabel.getId(), owner.getId(), "Work"))
                .isInstanceOf(DuplicateLabelNameException.class);
        assertThatThrownBy(() -> service.updateLabel(secondLabel.getId(), owner.getId(), " "))
                .isInstanceOf(InvalidLabelNameException.class);
        assertThat(service.getAllLabels(owner.getId())).extracting(LabelResult::labelName)
                .containsExactlyInAnyOrder("Work", "work");
    }

    @Test
    @DisplayName("사전 조회를 우회한 이름 충돌도 JPA flush에서 도메인 예외로 변환한다")
    void create_RejectsDuplicate_DatabaseConstraint() {
        service.createLabel(owner.getId(), "Work");

        assertThatThrownBy(() -> labelRepository.create(Label.restore("another-id", "Work", owner.getId(), Set.of())))
                .isInstanceOf(DuplicateLabelNameException.class);
    }

    @Test
    @DisplayName("비소유자의 삭제를 거부하고 소유자 삭제는 멤버십도 없앤다")
    void deleteLabel_DeletesMemberships_OwnerOnly() {
        // given
        Label label = service.createLabel(owner.getId(), "친구");
        connect(owner, first);
        service.addMemberToLabel(owner.getId(), label.getId(), first.getId());

        // when & then
        assertThatThrownBy(() -> service.deleteLabel(first.getId(), label.getId()))
                .isInstanceOf(LabelAuthorizationException.class);
        assertThat(labelJdbcRepository.existsMember(label.getId(), first.getId())).isTrue();
        service.deleteLabel(owner.getId(), label.getId());
        assertThat(labelJdbcRepository.existsMember(label.getId(), first.getId())).isFalse();
        assertThatThrownBy(() -> service.getLabelById(owner.getId(), label.getId()))
                .isInstanceOf(LabelNotFoundException.class);
    }

    @Test
    @DisplayName("친구만 추가할 수 있고 같은 멤버는 중복 등록할 수 없다")
    void addMember_RejectsNonFriendAndDuplicate() {
        // given
        Label label = service.createLabel(owner.getId(), "친구");
        connect(owner, first);
        service.addMemberToLabel(owner.getId(), label.getId(), first.getId());

        // when & then
        assertThatThrownBy(() -> service.addMemberToLabel(owner.getId(), label.getId(), first.getId()))
                .isInstanceOf(DuplicateLabelMemberException.class);
        assertThatThrownBy(() -> service.addMemberToLabel(owner.getId(), label.getId(), second.getId()))
                .isInstanceOf(FriendshipNotFoundException.class);
        assertThat(service.getLabelMembers(owner.getId(), label.getId())).hasSize(1);
        service.removeMemberFromLabel(owner.getId(), label.getId(), first.getId());
        assertThat(service.getLabelMembers(owner.getId(), label.getId())).isEmpty();
    }

    @Test
    @DisplayName("비활성 친구를 멤버로 추가하면 관계 없음이 아니라 사용자 없음으로 거부한다")
    void addMember_RejectsInactiveFriend_AsMissingUser() {
        // given
        Label label = service.createLabel(owner.getId(), "친구");
        connect(owner, first);
        first.deactivate();
        em.flush();

        // when & then
        assertThatThrownBy(() -> service.addMemberToLabel(owner.getId(), label.getId(), first.getId()))
                .isInstanceOf(UserReferenceNotFoundException.class);
        assertThat(service.getLabelMembers(owner.getId(), label.getId())).isEmpty();
    }

    @Test
    @DisplayName("멤버 교체는 중복을 제거하고 빈 목록은 라벨을 비운다")
    void replaceMembers_StoresExactlyTheRequestedFriends() {
        // given
        Label label = service.createLabel(owner.getId(), "친구");
        connect(owner, first);
        connect(owner, second);
        service.addMemberToLabel(owner.getId(), label.getId(), first.getId());

        // when
        service.replaceLabelMembers(owner.getId(), label.getId(), List.of(second.getId(), second.getId()));

        // then
        assertThat(service.getLabelMembers(owner.getId(), label.getId()))
                .extracting(LabelMemberResult::id).containsExactly(second.getId());
        assertThatThrownBy(() -> service.replaceLabelMembers(owner.getId(), label.getId(), List.of(owner.getId())))
                .isInstanceOf(NonFriendLabelMemberException.class);
        assertThat(service.getLabelById(owner.getId(), label.getId()).memberCount()).isEqualTo(1);
        service.replaceLabelMembers(owner.getId(), label.getId(), List.of());
        assertThat(service.getLabelMembers(owner.getId(), label.getId())).isEmpty();
    }

    @Test
    @DisplayName("Buzz는 본인 라벨의 활성 멤버만 중복 없이 받는다")
    void fetchRecipientIds_FiltersForeignLabelsAndInactiveMembers() {
        // given
        var buzz = new LabelRecipientStrategyAdapter(service);
        Label firstLabel = service.createLabel(owner.getId(), "첫 라벨");
        Label secondLabel = service.createLabel(owner.getId(), "둘째 라벨");
        Label foreignLabel = service.createLabel(first.getId(), "다른 사람의 라벨");
        connect(owner, first);
        connect(owner, second);
        service.addMemberToLabel(owner.getId(), firstLabel.getId(), first.getId());
        service.replaceLabelMembers(owner.getId(), secondLabel.getId(), List.of(first.getId(), second.getId()));
        service.addMemberToLabel(first.getId(), foreignLabel.getId(), owner.getId());
        second.deactivate();
        em.flush();

        // when
        var recipients = buzz.fetchRecipientIds(owner.getId(),
                new LabelRecipientSpec(List.of(firstLabel.getId(), secondLabel.getId(), foreignLabel.getId())));

        // then
        assertThat(recipients).containsExactly(first.getId());
        assertThat(service.getLabelById(owner.getId(), secondLabel.getId()).memberCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("영속성 컨텍스트를 비운 뒤에도 이름과 멤버 교체·제거 결과를 복원한다")
    void saveChanges_RestoresState_AfterReload() {
        // given
        String labelId = service.createLabel(owner.getId(), "친구").getId();
        connect(owner, first);
        connect(owner, second);
        service.replaceLabelMembers(owner.getId(), labelId, List.of(first.getId(), second.getId()));
        em.clear();

        // when
        service.updateLabel(labelId, owner.getId(), "동료");
        service.replaceLabelMembers(owner.getId(), labelId, List.of(second.getId()));
        em.clear();

        // then
        Label loaded = labelRepository.findById(labelId).orElseThrow();
        assertThat(loaded.getLabelName()).isEqualTo("동료");
        assertThat(loaded.getMemberIds()).containsExactly(second.getId());
        service.removeMemberFromLabel(owner.getId(), labelId, second.getId());
        em.clear();
        assertThat(labelRepository.findById(labelId).orElseThrow().getMemberIds()).isEmpty();
    }

    @Test
    @DisplayName("명령 모델은 비활성 멤버도 복원하고 화면 집계는 활성 멤버만 보여 준다")
    void findById_PreservesInactiveMembers_CommandModel() {
        // given
        String labelId = service.createLabel(owner.getId(), "친구").getId();
        connect(owner, first);
        connect(owner, second);
        service.addMemberToLabel(owner.getId(), labelId, first.getId());
        first.deactivate();
        em.flush();
        em.clear();

        // when
        service.addMemberToLabel(owner.getId(), labelId, second.getId());
        em.clear();

        // then
        assertThat(labelRepository.findById(labelId).orElseThrow().getMemberIds())
                .containsExactlyInAnyOrder(first.getId(), second.getId());
        assertThat(service.getLabelById(owner.getId(), labelId).memberCount()).isEqualTo(1);
        assertThat(service.getLabelMembers(owner.getId(), labelId))
                .containsExactly(new LabelMemberResult(second.getId(), "second"));
    }

    @Test
    @DisplayName("단건 추가가 JPA로 읽은 다른 멤버십의 외부 삭제를 복원하지 않는다")
    void addMember_PreservesCleanup_ExistingSnapshot() {
        // given
        String labelId = service.createLabel(owner.getId(), "친구").getId();
        connect(owner, first);
        connect(owner, second);
        service.addMemberToLabel(owner.getId(), labelId, first.getId());
        em.clear();
        Label label = labelRepository.findById(labelId).orElseThrow();
        assertThat(label.getMemberIds()).containsExactly(first.getId());
        jdbc.update("DELETE FROM label_members WHERE label_id = ? AND member_id = ?", labelId, first.getId());

        // when
        service.addMemberToLabel(owner.getId(), labelId, second.getId());
        em.clear();

        // then
        assertThat(labelRepository.findById(labelId).orElseThrow().getMemberIds()).containsExactly(second.getId());
    }

    private User createUser(String name) {
        return em.persistAndFlush(User.createActive(name + "@mysql-labels.test", name));
    }

    @Test
    @DisplayName("소유자가 비활성이면 명령과 화면 조회에서 라벨을 찾을 수 없다")
    void findById_ReturnsEmpty_InactiveOwner() {
        // given
        String labelId = service.createLabel(owner.getId(), "친구").getId();
        connect(owner, first);
        service.addMemberToLabel(owner.getId(), labelId, first.getId());
        owner.deactivate();
        em.flush();
        em.clear();

        // when & then
        assertThat(labelRepository.findById(labelId)).isEmpty();
        assertThat(labelRepository.findOwnerId(labelId)).isEmpty();
        assertThat(labelJdbcRepository.findAllByOwner(owner.getId())).isEmpty();
        assertThat(labelJdbcRepository.findResultById(labelId)).isEmpty();
        assertThat(labelJdbcRepository.findMembers(labelId)).isEmpty();
        assertThatThrownBy(() -> service.updateLabel(labelId, owner.getId(), "동료"))
                .isInstanceOf(LabelNotFoundException.class);
    }

    private void connect(User one, User two) {
        friendships.create(Friendship.restore(LocalDate.of(2026, 1, 1), recognition(one), recognition(two)));
    }

    private static FriendRecognition recognition(User user) {
        return FriendRecognition.restore(user.getId(), null, FriendRecognition.INITIAL_RAW_SCORE,
                LocalDateTime.now(), false, true);
    }
}
