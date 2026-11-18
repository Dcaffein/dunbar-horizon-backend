package com.example.DunbarHorizon.social.adapter.out.persistence;

import com.example.DunbarHorizon.account.adapter.out.persistence.jpa.UserJpaRepository;
import com.example.DunbarHorizon.account.domain.User;
import com.example.DunbarHorizon.social.adapter.out.persistence.jdbc.LabelJdbcRepository;
import com.example.DunbarHorizon.social.application.port.out.LabelMemberFriendshipLookup;
import com.example.DunbarHorizon.social.application.service.LabelCommandService;
import com.example.DunbarHorizon.social.domain.label.Label;
import com.example.DunbarHorizon.social.domain.label.LabelMemberRegistrar;
import com.example.DunbarHorizon.social.domain.label.LabelNamer;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@JpaRepositoryTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({LabelCommandService.class, LabelPersistenceAdapter.class, LabelJdbcRepository.class,
        ActiveUserPersistenceAdapter.class, LabelNamer.class, LabelMemberRegistrar.class})
class LabelDeletionTest {

    @Autowired private UserJpaRepository userJpaRepository;
    @Autowired private LabelCommandService labelCommandService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @MockitoBean private LabelMemberFriendshipLookup labelMemberFriendshipLookup;
    @MockitoSpyBean private LabelPersistenceAdapter labelPersistenceAdapter;

    private Long ownerId;
    private Long memberId;
    private String labelId;

    @BeforeEach
    void setUp() {
        ownerId = userJpaRepository.save(User.createActive("owner@label-deletion.test", "owner")).getId();
        memberId = userJpaRepository.save(User.createActive("member@label-deletion.test", "member")).getId();
        labelId = labelCommandService.createLabel(ownerId, "친구").getId();
        jdbcTemplate.update("INSERT INTO label_members (label_id, member_id) VALUES (?, ?)", labelId, memberId);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM label_members WHERE label_id = ?", labelId);
        jdbcTemplate.update("DELETE FROM labels WHERE id = ?", labelId);
        userJpaRepository.deleteAllById(List.of(ownerId, memberId));
    }

    @Test
    @DisplayName("CASCADE 외래 키 없이도 JPA 삭제가 라벨과 멤버십을 함께 지운다")
    void deleteLabel_RemovesMemberships_WithoutForeignKey() {
        // when
        labelCommandService.deleteLabel(ownerId, labelId);

        // then
        assertRows(0);
    }

    @Test
    @DisplayName("JPA 삭제 SQL 실행 뒤 실패하면 라벨과 멤버십이 함께 복원된다")
    void deleteLabel_RollsBack_AfterDeleteStatements() {
        // given
        doAnswer(call -> {
            call.callRealMethod();
            throw new IllegalStateException("interrupted after label deletion");
        }).when(labelPersistenceAdapter).delete(any(Label.class));

        // when & then
        assertThatThrownBy(() -> labelCommandService.deleteLabel(ownerId, labelId)).isInstanceOf(IllegalStateException.class);
        assertRows(1);
    }

    private void assertRows(long expected) {
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM labels WHERE id = ?", Long.class, labelId))
                .isEqualTo(expected);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM label_members WHERE label_id = ?", Long.class, labelId))
                .isEqualTo(expected);
    }
}
