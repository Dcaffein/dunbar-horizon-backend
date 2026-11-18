package com.example.DunbarHorizon.social.adapter.out.persistence;

import com.example.DunbarHorizon.account.adapter.out.persistence.jpa.UserJpaRepository;
import com.example.DunbarHorizon.account.domain.User;
import com.example.DunbarHorizon.social.adapter.out.persistence.jdbc.FriendshipJdbcRepository;
import com.example.DunbarHorizon.social.adapter.out.persistence.jdbc.LabelJdbcRepository;
import com.example.DunbarHorizon.social.application.dto.command.FriendshipUpdateCommand;
import com.example.DunbarHorizon.social.application.eventListener.LabelMembershipEventListener;
import com.example.DunbarHorizon.social.application.service.FriendshipCommandService;
import com.example.DunbarHorizon.social.application.service.LabelCommandService;
import com.example.DunbarHorizon.social.domain.friend.FriendRecognition;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.event.FriendshipEndedEvent;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendshipNotFoundException;
import com.example.DunbarHorizon.social.domain.friend.exception.InvalidFriendAliasException;
import com.example.DunbarHorizon.social.domain.label.LabelNamer;
import com.example.DunbarHorizon.social.domain.label.LabelMemberRegistrar;
import com.example.DunbarHorizon.support.JpaRepositoryTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;

@JpaRepositoryTest
@RecordApplicationEvents
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({FriendshipJdbcRepository.class, FriendshipPersistenceAdapter.class, LabelPersistenceAdapter.class, LabelJdbcRepository.class, ActiveUserPersistenceAdapter.class,
        FriendshipCommandService.class, LabelCommandService.class, LabelNamer.class, LabelMemberRegistrar.class,
        LabelMembershipEventListener.class})
class FriendshipLifecycleTest {

    private static final LocalDate FRIENDS_SINCE = LocalDate.of(2026, 1, 1);

    @Autowired private UserJpaRepository users;
    @Autowired private FriendshipCommandService lifecycle;
    @Autowired private LabelCommandService labelCommandService;
    @MockitoSpyBean private LabelJdbcRepository labels;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;
    @MockitoSpyBean private FriendshipPersistenceAdapter friendships;
    @Autowired private ApplicationEvents applicationEvents;

    private Long first;
    private Long second;
    private String firstLabel;
    private String secondLabel;

    @BeforeEach
    void setUp() {
        first = users.save(User.createActive("first@mysql-friendship-lifecycle.test", "first")).getId();
        second = users.save(User.createActive("second@mysql-friendship-lifecycle.test", "second")).getId();
        firstLabel = labelCommandService.createLabel(first, "내 라벨").getId();
        secondLabel = labelCommandService.createLabel(second, "상대 라벨").getId();
    }

    @AfterEach
    void tearDown() {
        labelCommandService.deleteLabel(first, firstLabel);
        labelCommandService.deleteLabel(second, secondLabel);
        jdbc.update("DELETE FROM friendships WHERE low_user_id = ? AND high_user_id = ?",
                Math.min(first, second), Math.max(first, second));
        users.deleteAllById(List.of(first, second));
    }

    @Test
    @DisplayName("친구 관계 삭제 이벤트가 동기로 양쪽 라벨을 정리하고 삭제 실패 시에는 발행되지 않는다")
    void brokeUpWith_RemovesBothMemberships_WithFriendship() {
        // given
        connectWithBothMemberships(FriendRecognition.INITIAL_RAW_SCORE);
        lifecycle.updateSettings(second, first, new FriendshipUpdateCommand(second, first, "친구", null, null));
        assertThat(friendships.findBetween(first, second).orElseThrow().getFriendAlias(second)).isEqualTo("친구");

        // when
        lifecycle.endFriendship(second, first);

        // then
        assertEnded();
        assertThatThrownBy(() -> lifecycle.endFriendship(first, second)).isInstanceOf(FriendshipNotFoundException.class);
        assertThat(applicationEvents.stream(FriendshipEndedEvent.class).toList())
                .containsExactly(new FriendshipEndedEvent(second, first));
    }

    @Test
    @DisplayName("친구 관계 삭제 이벤트의 멤버십 정리가 실패하면 관계와 양쪽 멤버십이 함께 복원된다")
    void brokeUpWith_RollsBackBothMemberships_OnFailure() {
        // given
        connectWithBothMemberships(FriendRecognition.INITIAL_RAW_SCORE);
        failAfterLabelCleanup();

        // when & then
        assertThatThrownBy(() -> lifecycle.endFriendship(first, second)).isInstanceOf(IllegalStateException.class);
        assertRelationshipAndMembershipsRemain();
    }

    private void failAfterLabelCleanup() {
        doAnswer(call -> {
            call.callRealMethod();
            throw new IllegalStateException("interrupted after label cleanup");
        }).when(labels).removeMembershipsBetween(first, second);
    }

    @Test
    @DisplayName("관계가 없거나 같은 사용자를 지정하면 설정 변경은 관계 없음 예외로 끝난다")
    void updateFriendship_ThrowsNotFound_MissingRelationship() {
        assertThatThrownBy(() -> lifecycle.updateSettings(first, second,
                new FriendshipUpdateCommand(first, second, null, true, null)))
                .isInstanceOf(FriendshipNotFoundException.class);
        assertThatThrownBy(() -> lifecycle.updateSettings(first, first,
                new FriendshipUpdateCommand(first, first, null, null, null)))
                .isInstanceOf(FriendshipNotFoundException.class);
    }

    @Test
    @DisplayName("별명 검증에 실패하면 함께 요청한 다른 설정도 바뀌지 않는다")
    void updateFriendship_RejectsEntireUpdate_InvalidAlias() {
        // given
        connect(FriendRecognition.INITIAL_RAW_SCORE);

        // when & then
        assertThatThrownBy(() -> lifecycle.updateSettings(second, first, new FriendshipUpdateCommand(second, first,
                "a".repeat(Friendship.ALIAS_MAX_LENGTH + 1), true, null)))
                .isInstanceOf(InvalidFriendAliasException.class);
        assertThat(friendships.findBetween(first, second).orElseThrow().isMuted(second)).isFalse();
    }

    @Test
    @DisplayName("설정 변경은 요청한 필드만 바꾸고 빈 별명은 별명을 지운다")
    void updateFriendship_ChangesRequestedFields_ClearsEmptyAlias() {
        // given
        connect(FriendRecognition.INITIAL_RAW_SCORE);
        lifecycle.updateSettings(first, second, new FriendshipUpdateCommand(first, second, "친구", true, null));

        // when
        lifecycle.updateSettings(first, second, new FriendshipUpdateCommand(first, second, "", null, false));

        // then
        Friendship updated = friendships.findBetween(first, second).orElseThrow();
        assertThat(updated.getFriendAlias(first)).isEmpty();
        assertThat(updated.isMuted(first)).isTrue();
        assertThat(updated.isRoutable(first)).isFalse();
        assertThat(updated.isMuted(second)).isFalse();
        assertThat(updated.isRoutable(second)).isTrue();
    }

    private void connect(double interestScore) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                friendships.create(Friendship.restore(FRIENDS_SINCE,
                        FriendRecognition.restore(first, null, interestScore, LocalDateTime.now(), false, true),
                        FriendRecognition.restore(second, null, interestScore, LocalDateTime.now(), false, true))));
    }

    private void connectWithBothMemberships(double interestScore) {
        connect(interestScore);
        labelCommandService.addMemberToLabel(first, firstLabel, second);
        labelCommandService.addMemberToLabel(second, secondLabel, first);
    }

    private void assertEnded() {
        assertThat(friendships.existsBetween(first, second)).isFalse();
        assertThat(labels.existsMember(firstLabel, second)).isFalse();
        assertThat(labels.existsMember(secondLabel, first)).isFalse();
    }

    private void assertRelationshipAndMembershipsRemain() {
        assertThat(friendships.existsBetween(first, second)).isTrue();
        assertThat(labels.existsMember(firstLabel, second)).isTrue();
        assertThat(labels.existsMember(secondLabel, first)).isTrue();
    }
}
