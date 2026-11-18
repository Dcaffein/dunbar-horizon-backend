package com.example.DunbarHorizon.social.adapter.out.persistence;

import com.example.DunbarHorizon.account.adapter.out.persistence.jpa.UserJpaRepository;
import com.example.DunbarHorizon.account.domain.User;
import com.example.DunbarHorizon.social.adapter.out.persistence.jdbc.LabelJdbcRepository;
import com.example.DunbarHorizon.social.application.port.out.LabelMemberFriendshipLookup;
import com.example.DunbarHorizon.social.application.port.out.LabelMemberFriendshipLookup.Candidates;
import com.example.DunbarHorizon.social.application.service.LabelCommandService;
import com.example.DunbarHorizon.social.domain.label.Label;
import com.example.DunbarHorizon.social.domain.label.LabelMemberRegistrar;
import com.example.DunbarHorizon.social.domain.label.LabelNamer;
import com.example.DunbarHorizon.social.domain.label.exception.DuplicateLabelMemberException;
import com.example.DunbarHorizon.social.domain.label.exception.LabelConflictException;
import com.example.DunbarHorizon.support.JpaRepositoryTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;

@JpaRepositoryTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({LabelCommandService.class, LabelPersistenceAdapter.class, LabelJdbcRepository.class,
        LabelNamer.class, LabelMemberRegistrar.class})
class LabelMembershipConcurrencyTest {

    @Autowired private UserJpaRepository userJpaRepository;
    @Autowired private LabelCommandService service;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private LabelMemberFriendshipLookup labelMemberFriendshipLookup;
    @MockitoSpyBean private LabelMemberRegistrar labelMemberRegistrar;

    private User owner;
    private User first;
    private User second;
    private User third;
    private String labelId;

    @BeforeEach
    void setUp() {
        owner = createUser("owner");
        first = createUser("first");
        second = createUser("second");
        third = createUser("third");
        given(labelMemberFriendshipLookup.findCandidatesWithSharedLock(anyLong(), anyCollection()))
                .willAnswer(call -> {
                    Set<Long> ids = Set.copyOf(call.<Collection<Long>>getArgument(1));
                    return new Candidates(ids, ids);
                });
        labelId = service.createLabel(owner.getId(), "친구").getId();
    }

    @AfterEach
    void tearDown() {
        List<Long> userIds = List.of(owner.getId(), first.getId(), second.getId(), third.getId());
        jdbc.update("DELETE FROM label_members WHERE label_id = ?", labelId);
        jdbc.update("DELETE FROM labels WHERE id = ?", labelId);
        userJpaRepository.deleteAllById(userIds);
    }

    @Test
    @DisplayName("같은 멤버를 동시에 추가하면 하나만 저장되고 나머지는 중복 또는 충돌로 거부된다")
    void addMember_RejectsOneRequest_WhenSameMemberAddedConcurrently() throws Exception {
        // given
        var bothAdded = new CyclicBarrier(2);
        doAnswer(call -> {
            call.callRealMethod();
            bothAdded.await(10, TimeUnit.SECONDS);
            return null;
        }).when(labelMemberRegistrar).addMember(any(Label.class), eq(first.getId()), anySet());

        // when
        List<Throwable> failures = runConcurrently(
                () -> service.addMemberToLabel(owner.getId(), labelId, first.getId()),
                () -> service.addMemberToLabel(owner.getId(), labelId, first.getId()));

        // then
        assertThat(failures).singleElement()
                .isInstanceOfAny(DuplicateLabelMemberException.class, LabelConflictException.class);
        assertThat(memberIds()).containsExactly(first.getId());
    }

    @Test
    @DisplayName("라벨 삭제가 먼저 커밋되면 이미 라벨을 읽은 멤버 추가는 충돌로 실패하고 멤버 행을 남기지 않는다")
    void addMember_FailsWithoutOrphan_WhenLabelDeletedFirst() throws Exception {
        // given
        var added = new CountDownLatch(1);
        var deleted = new CountDownLatch(1);
        doAnswer(call -> {
            call.callRealMethod();
            added.countDown();
            assertThat(deleted.await(10, TimeUnit.SECONDS)).isTrue();
            return null;
        }).when(labelMemberRegistrar).addMember(any(Label.class), eq(first.getId()), anySet());

        // when
        List<Throwable> failures = runConcurrently(
                () -> service.addMemberToLabel(owner.getId(), labelId, first.getId()),
                () -> {
                    try {
                        assertThat(added.await(10, TimeUnit.SECONDS)).isTrue();
                        service.deleteLabel(owner.getId(), labelId);
                    } catch (InterruptedException e) {
                        throw new IllegalStateException(e);
                    } finally {
                        deleted.countDown();
                    }
                });

        // then
        assertThat(failures).singleElement().isInstanceOf(LabelConflictException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM labels WHERE id = ?", Long.class, labelId)).isZero();
        assertThat(memberIds()).isEmpty();
    }

    @Test
    @DisplayName("멤버 교체가 읽은 뒤 추가가 먼저 커밋되면 교체는 충돌로 실패하고 추가 결과가 남는다")
    void replaceMembers_FailsWithConflict_WhenMemberAddedAfterSnapshot() throws Exception {
        // given
        service.addMemberToLabel(owner.getId(), labelId, first.getId());
        var replaced = new CountDownLatch(1);
        var added = new CountDownLatch(1);
        doAnswer(call -> {
            call.callRealMethod();
            replaced.countDown();
            assertThat(added.await(10, TimeUnit.SECONDS)).isTrue();
            return null;
        }).when(labelMemberRegistrar).replaceMembers(any(Label.class), anyCollection(), anySet());

        // when
        List<Throwable> failures = runConcurrently(
                () -> service.replaceLabelMembers(owner.getId(), labelId, List.of(second.getId())),
                () -> {
                    try {
                        assertThat(replaced.await(10, TimeUnit.SECONDS)).isTrue();
                        service.addMemberToLabel(owner.getId(), labelId, third.getId());
                    } catch (InterruptedException e) {
                        throw new IllegalStateException(e);
                    } finally {
                        added.countDown();
                    }
                });

        // then
        assertThat(failures).singleElement().isInstanceOf(LabelConflictException.class);
        assertThat(memberIds()).containsExactlyInAnyOrder(first.getId(), third.getId());
    }

    private User createUser(String name) {
        return userJpaRepository.save(User.createActive(name + "@label-membership.test", name));
    }

    private List<Long> memberIds() {
        return jdbc.queryForList("SELECT member_id FROM label_members WHERE label_id = ?", Long.class, labelId);
    }

    private List<Throwable> runConcurrently(Runnable first, Runnable second) throws Exception {
        try (var workers = Executors.newFixedThreadPool(2)) {
            var firstFailure = workers.submit(() -> catchThrowable(first::run));
            var secondFailure = workers.submit(() -> catchThrowable(second::run));
            return Stream.of(firstFailure.get(20, TimeUnit.SECONDS), secondFailure.get(20, TimeUnit.SECONDS))
                    .filter(Objects::nonNull)
                    .toList();
        }
    }
}
