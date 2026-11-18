package com.example.DunbarHorizon.social.adapter.out.persistence.label;

import com.example.DunbarHorizon.account.adapter.out.persistence.jpa.UserJpaRepository;
import com.example.DunbarHorizon.account.domain.User;
import com.example.DunbarHorizon.social.application.dto.result.LabelResult;
import com.example.DunbarHorizon.social.application.port.out.LabelMemberFriendshipLookup;
import com.example.DunbarHorizon.social.application.service.LabelCommandService;
import com.example.DunbarHorizon.social.domain.label.Label;
import com.example.DunbarHorizon.social.domain.label.LabelMemberRegistrar;
import com.example.DunbarHorizon.social.domain.label.LabelName;
import com.example.DunbarHorizon.social.domain.label.LabelNamer;
import com.example.DunbarHorizon.social.domain.label.exception.DuplicateLabelNameException;
import com.example.DunbarHorizon.social.domain.label.exception.InvalidLabelNameException;
import com.example.DunbarHorizon.social.domain.label.exception.LabelNotFoundException;
import com.example.DunbarHorizon.social.domain.socialUser.exception.UserReferenceNotFoundException;
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

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@JpaRepositoryTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({LabelCommandService.class, LabelPersistenceAdapter.class, LabelMembershipCleanupJdbcAdapter.class,
        LabelNamer.class, LabelMemberRegistrar.class})
class LabelNamingConcurrencyTest {

    @Autowired private UserJpaRepository userJpaRepository;
    @Autowired private LabelCommandService service;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private LabelMemberFriendshipLookup labelMemberFriendshipLookup;
    @MockitoSpyBean private LabelNamer labelNamer;

    private User owner;
    private User other;

    @BeforeEach
    void setUp() {
        owner = userJpaRepository.save(User.createActive("owner@label-naming.test", "owner"));
        other = userJpaRepository.save(User.createActive("other@label-naming.test", "other"));
    }

    @AfterEach
    void tearDown() {
        jdbc.update("DELETE FROM labels WHERE owner_id IN (?, ?)", owner.getId(), other.getId());
        userJpaRepository.deleteAllById(List.of(owner.getId(), other.getId()));
    }

    @Test
    @DisplayName("동시 생성이 같은 이름을 선택하면 한 라벨만 저장되고 나머지 요청은 중복 예외를 받는다")
    void createLabel_RejectsOneRequest_WhenBothRequestsChooseSameName() throws Exception {
        // given
        synchronizeFirstTwoAssignments("친구");

        // when
        List<Throwable> failures = runConcurrently(
                () -> service.createLabel(owner.getId(), "친구"),
                () -> service.createLabel(owner.getId(), "친구"));

        // then
        assertThat(failures).singleElement().isInstanceOf(DuplicateLabelNameException.class);
        assertThat(namesOf(owner)).containsExactly("친구");
        verify(labelNamer, times(2)).assign(eq("친구"), anySet());
    }

    @Test
    @DisplayName("두 라벨의 이름 변경이 경합하면 실패한 라벨은 원래 이름을 유지한다")
    void updateLabel_PreservesOriginalName_WhenRequestLosesNameConflict() throws Exception {
        // given
        String firstId = service.createLabel(owner.getId(), "첫 라벨").getId();
        String secondId = service.createLabel(owner.getId(), "둘째 라벨").getId();
        synchronizeFirstTwoAssignments("친구");

        // when
        List<Throwable> failures = runConcurrently(
                () -> service.updateLabel(firstId, owner.getId(), "친구"),
                () -> service.updateLabel(secondId, owner.getId(), "친구"));

        // then
        assertThat(failures).singleElement().isInstanceOf(DuplicateLabelNameException.class);
        assertThat(List.of(nameOf(firstId), nameOf(secondId)))
                .isIn(List.of("친구", "둘째 라벨"), List.of("첫 라벨", "친구"));
        verify(labelNamer, times(2)).assign(eq("친구"), anySet());
    }

    @Test
    @DisplayName("소유자별로 빈 번호를 고르고 자기 이름으로 변경하면 이름을 유지한다")
    void assign_UsesOnlyOtherLabels_OfSameOwner() {
        // given
        service.createLabel(other.getId(), "Work");
        service.createLabel(other.getId(), "Work (2)");
        Label work = service.createLabel(owner.getId(), "Work");
        Label lowerCase = service.createLabel(owner.getId(), "work");
        service.createLabel(owner.getId(), "Work (3)");

        // when
        service.updateLabel(work.getId(), owner.getId(), "Work");
        service.updateLabel(lowerCase.getId(), owner.getId(), "Work");

        // then
        assertThat(namesOf(owner)).containsExactlyInAnyOrder("Work", "Work (2)", "Work (3)");
        assertThat(nameOf(work.getId())).isEqualTo("Work");
        assertThat(nameOf(lowerCase.getId())).isEqualTo("Work (2)");
    }

    @Test
    @DisplayName("공백으로 이름을 바꾸거나 길이 제한을 넘는 이름으로 생성할 수 없다")
    void assign_RejectsInvalidName_WithoutChangingStoredLabels() {
        // given
        String labelId = service.createLabel(owner.getId(), "친구").getId();

        // when & then
        assertThatThrownBy(() -> service.updateLabel(labelId, owner.getId(), " "))
                .isInstanceOf(InvalidLabelNameException.class);
        assertThatThrownBy(() -> service.createLabel(owner.getId(), "가".repeat(LabelName.MAX_LENGTH + 1)))
                .isInstanceOf(InvalidLabelNameException.class);
        assertThat(namesOf(owner)).containsExactly("친구");
    }

    @Test
    @DisplayName("20자 이름의 축약 경계에서 서로게이트 쌍을 잘라 내지 않고 저장한다")
    void createLabel_SavesShortenedName_WhenSuffixSplitsSurrogatePair() {
        // given
        String requestedName = "가".repeat(15) + "😀나다라";
        service.createLabel(owner.getId(), requestedName);

        // when
        Label created = service.createLabel(owner.getId(), requestedName);

        // then
        String shortenedName = "가".repeat(15) + " (2)";
        assertThat(LabelResult.created(created)).isEqualTo(new LabelResult(created.getId(), shortenedName, 0));
        assertThat(namesOf(owner)).containsExactlyInAnyOrder(requestedName, shortenedName);
    }

    @Test
    @DisplayName("비활성 소유자는 이름을 바꿀 수 없고 비활성이거나 없는 사용자에게 라벨을 생성할 수 없다")
    void writeLabel_Rejects_InactiveOrMissingOwner() {
        // given
        String labelId = service.createLabel(owner.getId(), "친구").getId();
        owner.deactivate();
        userJpaRepository.save(owner);

        // when & then
        assertThatThrownBy(() -> service.updateLabel(labelId, owner.getId(), "동료"))
                .isInstanceOf(LabelNotFoundException.class);
        assertThatThrownBy(() -> service.createLabel(owner.getId(), "동료"))
                .isInstanceOf(UserReferenceNotFoundException.class);
        assertThatThrownBy(() -> service.createLabel(Long.MAX_VALUE, "친구"))
                .isInstanceOf(UserReferenceNotFoundException.class);
        assertThat(namesOf(owner)).containsExactly("친구");
    }

    private List<String> namesOf(User user) {
        return jdbc.queryForList("SELECT name FROM labels WHERE owner_id = ?", String.class, user.getId());
    }

    private String nameOf(String labelId) {
        return jdbc.queryForObject("SELECT name FROM labels WHERE id = ?", String.class, labelId);
    }

    private void synchronizeFirstTwoAssignments(String requestedName) {
        var bothAssigned = new CyclicBarrier(2);
        var assignments = new AtomicInteger();
        doAnswer(call -> {
            LabelName assigned = (LabelName) call.callRealMethod();
            if (assignments.incrementAndGet() <= 2) {
                assertThat(assigned.value()).isEqualTo(requestedName);
                bothAssigned.await(10, TimeUnit.SECONDS);
            }
            return assigned;
        }).when(labelNamer).assign(eq(requestedName), anySet());
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
