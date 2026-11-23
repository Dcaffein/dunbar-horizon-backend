package com.example.DunbarHorizon.buzz.adapter.out.persistence;

import com.example.DunbarHorizon.buzz.application.dto.command.CreateBuzzCommand;
import com.example.DunbarHorizon.buzz.application.dto.command.recipient.ManualRecipientSpec;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzSocialPort;
import com.example.DunbarHorizon.buzz.application.port.out.ImageStoragePort;
import com.example.DunbarHorizon.buzz.application.service.BuzzProfileAssembler;
import com.example.DunbarHorizon.buzz.application.service.BuzzCommandService;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;

@JpaRepositoryTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({BuzzRepositoryAdapter.class, BuzzAudienceMemberRepositoryAdapter.class,
        BuzzQueryRepositoryAdapter.class, BuzzThreadRepositoryAdapter.class, BuzzCommentRepositoryAdapter.class, BuzzProfileAssembler.class,
        BuzzCommandService.class})
class BuzzCreationTransactionTest {

    @Autowired private BuzzCommandService buzzCommandService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @MockitoSpyBean private BuzzAudienceMemberRepositoryAdapter buzzAudienceMemberRepositoryAdapter;
    @MockitoBean private BuzzSocialPort buzzSocialPort;
    @MockitoBean private ImageStoragePort imageStoragePort;

    private final Long creatorId = 22_001L;
    private final Long recipientId = 22_002L;

    @BeforeEach
    void setUp() {
        given(buzzSocialPort.getFriendIdsIn(creatorId, List.of(recipientId))).willReturn(Set.of(recipientId));
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM buzz_audience_members WHERE user_id = ?", recipientId);
        jdbcTemplate.update("DELETE FROM buzzes WHERE creator_id = ?", creatorId);
    }

    @Test
    @DisplayName("공개 대상 멤버 저장이 실패하면 버즈도 저장되지 않는다")
    void createBuzz_MemberSaveFails_RollsBackBuzz() {
        // given
        doThrow(new IllegalStateException("멤버 저장 실패")).when(buzzAudienceMemberRepositoryAdapter).saveAll(anyList());

        // when & then
        assertThatThrownBy(() -> buzzCommandService.createBuzz(command(), List.of()))
                .isInstanceOf(IllegalStateException.class);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM buzzes WHERE creator_id = ?", Long.class, creatorId)).isZero();
    }

    @Test
    @DisplayName("생성이 성공하면 버즈와 미읽음 공개 대상 멤버가 함께 저장된다")
    void createBuzz_SavesBuzzAndUnreadMembers() {
        // when
        buzzCommandService.createBuzz(command(), List.of());

        // then
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM buzzes WHERE creator_id = ?", Long.class, creatorId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForList(
                "SELECT m.is_read FROM buzz_audience_members m JOIN buzzes b ON b.id = m.buzz_id "
                        + "WHERE b.creator_id = ?", Boolean.class, creatorId)).containsExactly(false);
    }

    private CreateBuzzCommand command() {
        return new CreateBuzzCommand(creatorId, "본문", new ManualRecipientSpec(List.of(recipientId)));
    }
}
