package com.example.DunbarHorizon.social.adapter.out.persistence.label;

import com.example.DunbarHorizon.account.domain.User;
import com.example.DunbarHorizon.buzz.adapter.out.social.LabelRecipientStrategyAdapter;
import com.example.DunbarHorizon.buzz.application.dto.info.recipient.LabelRecipientSpec;
import com.example.DunbarHorizon.social.adapter.out.persistence.friendship.FriendshipPersistenceAdapter;
import com.example.DunbarHorizon.social.application.label.dto.LabelMemberResult;
import com.example.DunbarHorizon.social.application.label.dto.LabelResult;
import com.example.DunbarHorizon.social.application.label.port.LabelQueryRepository;
import com.example.DunbarHorizon.social.application.label.port.LabelRepository;
import com.example.DunbarHorizon.social.application.label.LabelCommandService;
import com.example.DunbarHorizon.social.application.label.LabelQueryService;
import com.example.DunbarHorizon.social.domain.friendship.FriendRecognition;
import com.example.DunbarHorizon.social.domain.friendship.Friendship;
import com.example.DunbarHorizon.social.domain.friendship.exception.FriendshipNotFoundException;
import com.example.DunbarHorizon.social.domain.label.Label;
import com.example.DunbarHorizon.social.domain.label.LabelNamer;
import com.example.DunbarHorizon.social.domain.label.LabelMemberRegistrar;
import com.example.DunbarHorizon.social.domain.label.LabelTestFactory;
import com.example.DunbarHorizon.social.domain.label.exception.DuplicateLabelMemberException;
import com.example.DunbarHorizon.social.domain.label.exception.DuplicateLabelNameException;
import com.example.DunbarHorizon.social.domain.label.exception.LabelAuthorizationException;
import com.example.DunbarHorizon.social.domain.label.exception.LabelNotFoundException;
import com.example.DunbarHorizon.social.domain.label.exception.NonFriendLabelMemberException;
import com.example.DunbarHorizon.social.domain.exception.UserReferenceNotFoundException;
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

import static com.example.DunbarHorizon.social.adapter.out.persistence.support.LabelMembershipTestQueries.hasLabelMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JpaRepositoryTest
@Import({LabelQueryJdbcRepository.class, LabelMembershipCleanupJdbcAdapter.class, LabelMemberFriendshipLookupJpaAdapter.class,
        FriendshipPersistenceAdapter.class, LabelPersistenceAdapter.class,
        LabelCommandService.class, LabelQueryService.class, LabelNamer.class, LabelMemberRegistrar.class})
class LabelPersistenceAdapterTest {

    @Autowired private TestEntityManager em;
    @Autowired private LabelQueryRepository labelQueryRepository;
    @Autowired private LabelRepository labelRepository;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private FriendshipPersistenceAdapter friendshipPersistenceAdapter;
    @Autowired private LabelCommandService labelCommandService;
    @Autowired private LabelQueryService labelQueryService;

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
    @DisplayName("이름 조회는 이름이 없는 활성 소유자와 비활성 또는 없는 소유자를 구분한다")
    void findNamesOfActiveOwner_DistinguishesEmptyNames_FromUnavailableOwner() {
        // given
        first.deactivate();
        em.flush();

        // when & then
        assertThat(labelRepository.findNamesOfActiveOwner(owner.getId(), null)).contains(Set.of());
        assertThat(labelRepository.findNamesOfActiveOwner(first.getId(), null)).isEmpty();
        assertThat(labelRepository.findNamesOfActiveOwner(Long.MAX_VALUE, null)).isEmpty();
        assertThatThrownBy(() -> labelCommandService.createLabel(first.getId(), "친구"))
                .isInstanceOf(UserReferenceNotFoundException.class);

        Label label = labelCommandService.createLabel(owner.getId(), "친구");
        assertThat(labelRepository.findNamesOfActiveOwner(owner.getId(), null)).contains(Set.of("친구"));
        assertThat(labelRepository.findNamesOfActiveOwner(owner.getId(), label.getId())).contains(Set.of());
    }

    @Test
    @DisplayName("라벨을 생성하고 이름을 바꾸면 기존 응답으로 조회된다")
    void getLabel_ReturnsExistingResponse_AfterRename() {
        // given
        Label label = labelCommandService.createLabel(owner.getId(), "친구");
        connect(owner, first);
        labelCommandService.addMemberToLabel(owner.getId(), label.getId(), first.getId());

        // when
        labelCommandService.updateLabel(label.getId(), owner.getId(), "함께 운동");

        // then
        var expected = new LabelResult(label.getId(), "함께 운동", 1);
        assertThat(labelQueryService.getLabelById(owner.getId(), label.getId())).isEqualTo(expected);
        assertThat(labelQueryService.getAllLabels(owner.getId())).containsExactly(expected);
        assertThat(labelQueryService.getLabelsByMember(owner.getId(), first.getId())).containsExactly(expected);
        assertThat(labelQueryService.getLabelMembers(owner.getId(), label.getId()))
                .containsExactly(new LabelMemberResult(first.getId(), "first"));
        em.clear();
        assertThatThrownBy(() -> labelRepository.findById(label.getId()).orElseThrow().getMemberIds().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("사전 조회를 우회한 이름 충돌도 JPA flush에서 도메인 예외로 변환한다")
    void create_RejectsDuplicate_DatabaseConstraint() {
        labelCommandService.createLabel(owner.getId(), "Work");

        Label duplicate = LabelTestFactory.createLabel(owner.getId(), "Work");
        assertThatThrownBy(() -> labelRepository.save(duplicate))
                .isInstanceOf(DuplicateLabelNameException.class);
    }

    @Test
    @DisplayName("비소유자의 조회·삭제를 거부하고 소유자 삭제는 멤버십도 없앤다")
    void deleteLabel_DeletesMemberships_OwnerOnly() {
        // given
        Label label = labelCommandService.createLabel(owner.getId(), "친구");
        connect(owner, first);
        labelCommandService.addMemberToLabel(owner.getId(), label.getId(), first.getId());

        // when & then
        assertThatThrownBy(() -> labelQueryService.getLabelById(first.getId(), label.getId()))
                .isInstanceOf(LabelAuthorizationException.class);
        assertThatThrownBy(() -> labelQueryService.getLabelMembers(first.getId(), label.getId()))
                .isInstanceOf(LabelAuthorizationException.class);
        assertThatThrownBy(() -> labelCommandService.deleteLabel(first.getId(), label.getId()))
                .isInstanceOf(LabelAuthorizationException.class);
        assertThat(hasLabelMember(jdbc, label.getId(), first.getId())).isTrue();
        labelCommandService.deleteLabel(owner.getId(), label.getId());
        assertThat(hasLabelMember(jdbc, label.getId(), first.getId())).isFalse();
        assertThatThrownBy(() -> labelQueryService.getLabelById(owner.getId(), label.getId()))
                .isInstanceOf(LabelNotFoundException.class);
    }

    @Test
    @DisplayName("친구만 추가할 수 있고 같은 멤버는 중복 등록할 수 없다")
    void addMember_RejectsNonFriendAndDuplicate() {
        // given
        Label label = labelCommandService.createLabel(owner.getId(), "친구");
        connect(owner, first);
        labelCommandService.addMemberToLabel(owner.getId(), label.getId(), first.getId());

        // when & then
        assertThatThrownBy(() -> labelCommandService.addMemberToLabel(owner.getId(), label.getId(), first.getId()))
                .isInstanceOf(DuplicateLabelMemberException.class);
        assertThatThrownBy(() -> labelCommandService.addMemberToLabel(owner.getId(), label.getId(), second.getId()))
                .isInstanceOf(FriendshipNotFoundException.class);
        assertThatThrownBy(() -> labelCommandService.addMemberToLabel(owner.getId(), label.getId(), owner.getId()))
                .isInstanceOf(FriendshipNotFoundException.class);
        assertThat(labelQueryService.getLabelMembers(owner.getId(), label.getId())).hasSize(1);
        labelCommandService.removeMemberFromLabel(owner.getId(), label.getId(), first.getId());
        assertThat(labelQueryService.getLabelMembers(owner.getId(), label.getId())).isEmpty();
    }

    @Test
    @DisplayName("비활성 친구를 멤버로 추가하면 관계 없음이 아니라 사용자 없음으로 거부한다")
    void addMember_RejectsInactiveFriend_AsMissingUser() {
        // given
        Label label = labelCommandService.createLabel(owner.getId(), "친구");
        connect(owner, first);
        first.deactivate();
        em.flush();

        // when & then
        assertThatThrownBy(() -> labelCommandService.addMemberToLabel(owner.getId(), label.getId(), first.getId()))
                .isInstanceOf(UserReferenceNotFoundException.class);
        assertThatThrownBy(() -> labelCommandService.addMemberToLabel(owner.getId(), label.getId(), Long.MAX_VALUE))
                .isInstanceOf(UserReferenceNotFoundException.class);
        assertThat(labelQueryService.getLabelMembers(owner.getId(), label.getId())).isEmpty();
    }

    @Test
    @DisplayName("멤버 교체는 중복을 제거하고 빈 목록은 라벨을 비운다")
    void replaceMembers_StoresExactlyTheRequestedFriends() {
        // given
        Label label = labelCommandService.createLabel(owner.getId(), "친구");
        connect(owner, first);
        connect(owner, second);
        labelCommandService.addMemberToLabel(owner.getId(), label.getId(), first.getId());

        // when
        labelCommandService.replaceLabelMembers(owner.getId(), label.getId(), List.of(second.getId(), second.getId()));

        // then
        assertThat(labelQueryService.getLabelMembers(owner.getId(), label.getId()))
                .extracting(LabelMemberResult::id).containsExactly(second.getId());
        assertThatThrownBy(() -> labelCommandService.replaceLabelMembers(owner.getId(), label.getId(), List.of(owner.getId())))
                .isInstanceOf(NonFriendLabelMemberException.class);
        assertThat(labelQueryService.getLabelById(owner.getId(), label.getId()).memberCount()).isEqualTo(1);
        labelCommandService.replaceLabelMembers(owner.getId(), label.getId(), List.of());
        assertThat(labelQueryService.getLabelMembers(owner.getId(), label.getId())).isEmpty();
    }

    @Test
    @DisplayName("Buzz는 본인 라벨의 활성 멤버만 중복 없이 받는다")
    void fetchRecipientIds_FiltersForeignLabelsAndInactiveMembers() {
        // given
        var buzz = new LabelRecipientStrategyAdapter(labelQueryService);
        Label firstLabel = labelCommandService.createLabel(owner.getId(), "첫 라벨");
        Label secondLabel = labelCommandService.createLabel(owner.getId(), "둘째 라벨");
        Label foreignLabel = labelCommandService.createLabel(first.getId(), "다른 사람의 라벨");
        connect(owner, first);
        connect(owner, second);
        labelCommandService.addMemberToLabel(owner.getId(), firstLabel.getId(), first.getId());
        labelCommandService.replaceLabelMembers(owner.getId(), secondLabel.getId(), List.of(first.getId(), second.getId()));
        labelCommandService.addMemberToLabel(first.getId(), foreignLabel.getId(), owner.getId());
        second.deactivate();
        em.flush();

        // when
        var recipients = buzz.fetchRecipientIds(owner.getId(),
                new LabelRecipientSpec(List.of(firstLabel.getId(), secondLabel.getId(), foreignLabel.getId())));

        // then
        assertThat(recipients).containsExactly(first.getId());
        assertThat(labelQueryService.getLabelById(owner.getId(), secondLabel.getId()).memberCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("영속성 컨텍스트를 비운 뒤에도 이름과 멤버 교체·제거 결과를 복원한다")
    void save_RestoresState_AfterReload() {
        // given
        String labelId = labelCommandService.createLabel(owner.getId(), "친구").getId();
        connect(owner, first);
        connect(owner, second);
        labelCommandService.replaceLabelMembers(owner.getId(), labelId, List.of(first.getId(), second.getId()));
        em.clear();

        // when
        labelCommandService.updateLabel(labelId, owner.getId(), "동료");
        labelCommandService.replaceLabelMembers(owner.getId(), labelId, List.of(second.getId()));
        em.clear();

        // then
        Label loaded = labelRepository.findById(labelId).orElseThrow();
        assertThat(loaded.getLabelName()).isEqualTo("동료");
        assertThat(loaded.getMemberIds()).containsExactly(second.getId());
        labelCommandService.removeMemberFromLabel(owner.getId(), labelId, second.getId());
        em.clear();
        assertThat(labelRepository.findById(labelId).orElseThrow().getMemberIds()).isEmpty();
    }

    @Test
    @DisplayName("명령 모델은 비활성 멤버도 복원하고 화면 집계는 활성 멤버만 보여 준다")
    void findById_PreservesInactiveMembers_CommandModel() {
        // given
        String labelId = labelCommandService.createLabel(owner.getId(), "친구").getId();
        connect(owner, first);
        connect(owner, second);
        labelCommandService.addMemberToLabel(owner.getId(), labelId, first.getId());
        first.deactivate();
        em.flush();
        em.clear();

        // when
        labelCommandService.addMemberToLabel(owner.getId(), labelId, second.getId());
        em.clear();

        // then
        assertThat(labelRepository.findById(labelId).orElseThrow().getMemberIds())
                .containsExactlyInAnyOrder(first.getId(), second.getId());
        assertThat(labelQueryService.getLabelById(owner.getId(), labelId).memberCount()).isEqualTo(1);
        assertThat(labelQueryService.getLabelMembers(owner.getId(), labelId))
                .containsExactly(new LabelMemberResult(second.getId(), "second"));
    }

    @Test
    @DisplayName("단건 추가가 JPA로 읽은 다른 멤버십의 외부 삭제를 복원하지 않는다")
    void addMember_PreservesCleanup_ExistingSnapshot() {
        // given
        String labelId = labelCommandService.createLabel(owner.getId(), "친구").getId();
        connect(owner, first);
        connect(owner, second);
        labelCommandService.addMemberToLabel(owner.getId(), labelId, first.getId());
        em.clear();
        Label label = labelRepository.findById(labelId).orElseThrow();
        assertThat(label.getMemberIds()).containsExactly(first.getId());
        jdbc.update("DELETE FROM label_members WHERE label_id = ? AND member_id = ?", labelId, first.getId());

        // when
        labelCommandService.addMemberToLabel(owner.getId(), labelId, second.getId());
        em.clear();

        // then
        assertThat(labelRepository.findById(labelId).orElseThrow().getMemberIds()).containsExactly(second.getId());
    }

    @Test
    @DisplayName("소유자가 비활성이면 명령과 화면 조회에서 라벨을 찾을 수 없다")
    void findById_ReturnsEmpty_InactiveOwner() {
        // given
        String labelId = labelCommandService.createLabel(owner.getId(), "친구").getId();
        connect(owner, first);
        labelCommandService.addMemberToLabel(owner.getId(), labelId, first.getId());
        owner.deactivate();
        em.flush();
        em.clear();

        // when & then
        assertThat(labelRepository.findById(labelId)).isEmpty();
        assertThat(labelQueryRepository.findOwnerId(labelId)).isEmpty();
        assertThat(labelQueryRepository.findAllByOwner(owner.getId())).isEmpty();
        assertThat(labelQueryRepository.findById(labelId)).isEmpty();
        assertThat(labelQueryRepository.findMembers(labelId)).isEmpty();
    }

    @Test
    @DisplayName("새 라벨은 버전 0으로 저장되고 이름 변경과 멤버 추가가 각각 버전을 올린다")
    void save_IncrementsVersion_RenameAndMemberChange() {
        // given
        String labelId = labelCommandService.createLabel(owner.getId(), "친구").getId();
        connect(owner, first);
        long created = versionOf(labelId);

        // when
        labelCommandService.updateLabel(labelId, owner.getId(), "동료");
        long renamed = versionOf(labelId);
        labelCommandService.addMemberToLabel(owner.getId(), labelId, first.getId());

        // then
        assertThat(created).isZero();
        assertThat(renamed).isEqualTo(1);
        assertThat(versionOf(labelId)).isEqualTo(2);
    }

    @Test
    @DisplayName("새 라벨은 사전 조회 없이 넘긴 객체 그대로 영속 상태가 된다")
    void save_PersistsGivenInstance_NewLabel() {
        // given
        Label label = LabelTestFactory.createLabel(owner.getId(), "친구");

        // when
        labelRepository.save(label);

        // then
        assertThat(em.getEntityManager().contains(label)).isTrue();
        assertThat(versionOf(label.getId())).isZero();
    }

    @Test
    @DisplayName("저장 시점의 멤버 기본 키 위반을 멤버 중복 예외로 변환한다")
    void save_TranslatesPrimaryKeyViolation_DuplicateMember() {
        // given
        String labelId = labelCommandService.createLabel(owner.getId(), "친구").getId();
        connect(owner, first);
        assertThat(labelRepository.findById(labelId).orElseThrow().getMemberIds()).isEmpty();
        jdbc.update("INSERT INTO label_members (label_id, member_id) VALUES (?, ?)", labelId, first.getId());

        // when & then
        assertThatThrownBy(() -> labelCommandService.addMemberToLabel(owner.getId(), labelId, first.getId()))
                .isInstanceOf(DuplicateLabelMemberException.class);
    }

    private long versionOf(String labelId) {
        return jdbc.queryForObject("SELECT version FROM labels WHERE id = ?", Long.class, labelId);
    }

    private User createUser(String name) {
        return em.persistAndFlush(User.createActive(name + "@mysql-labels.test", name));
    }

    private void connect(User one, User two) {
        friendshipPersistenceAdapter.create(Friendship.restore(LocalDate.of(2026, 1, 1), recognition(one), recognition(two)));
    }

    private static FriendRecognition recognition(User user) {
        return FriendRecognition.restore(user.getId(), null, FriendRecognition.INITIAL_RAW_SCORE,
                LocalDateTime.now(), false, true);
    }
}
