package com.example.DunbarHorizon.flag.application.service.flag;

import com.example.DunbarHorizon.flag.adapter.out.persistence.jpa.FlagJpaRepository;
import com.example.DunbarHorizon.flag.application.port.in.FlagHostUseCase;
import com.example.DunbarHorizon.flag.application.port.in.FlagModificationUseCase;
import com.example.DunbarHorizon.flag.application.port.in.command.FlagEncoreCommand;
import com.example.DunbarHorizon.flag.application.port.in.command.FlagHostCommand;
import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.flag.FlagExpiryExemptionUpdater;
import com.example.DunbarHorizon.support.TestContainerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;

/**
 * 삭제와 부모 면제 갱신이 한 트랜잭션인지는 실제 커밋과 롤백으로만 확인할 수 있어
 * SpringBootTest로 실행하고 생성 데이터를 직접 정리한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainerConfig.class)
class FlagEncoreDeletionIntegrationTest {

    private static final Long HOST_ID = 13101L;

    @Autowired private FlagHostUseCase flagHostUseCase;
    @Autowired private FlagModificationUseCase flagModificationUseCase;
    @Autowired private FlagJpaRepository flagJpaRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    @MockitoSpyBean private FlagExpiryExemptionUpdater flagExpiryExemptionUpdater;

    private final List<Long> createdFlagIds = new ArrayList<>();

    private Long parentId;

    @BeforeEach
    void setUp() {
        LocalDateTime base = LocalDateTime.now().withNano(0);
        parentId = flagHostUseCase.hostFlag(new FlagHostCommand(
                HOST_ID, "종료된 모임", "설명", 10,
                base.minusHours(4), base.minusHours(3), base.minusHours(1)));
        createdFlagIds.add(parentId);
    }

    @AfterEach
    void tearDown() {
        new TransactionTemplate(transactionManager).executeWithoutResult(
                status -> flagJpaRepository.hardDeleteByIdsIn(createdFlagIds));
        createdFlagIds.clear();
    }

    @Test
    @DisplayName("앙코르를 삭제하면 요청이 끝난 시점에 부모의 만료 면제가 풀려 있다")
    void closeEncore_ReleasesParentExemptionBeforeReturning() {
        // given
        Long encoreId = encore();
        assertThat(parent().isAutoExpiryExempt()).isTrue();

        // when
        flagModificationUseCase.closeFlag(encoreId, HOST_ID);

        // then
        assertThat(parent().isAutoExpiryExempt()).isFalse();
    }

    @Test
    @DisplayName("부모 면제 갱신이 실패하면 앙코르 삭제도 롤백된다")
    void closeEncore_RollsBack_WhenParentRefreshFails() {
        // given
        Long encoreId = encore();
        doThrow(new IllegalStateException("갱신 실패")).when(flagExpiryExemptionUpdater).refresh(parentId);

        // when & then
        assertThatThrownBy(() -> flagModificationUseCase.closeFlag(encoreId, HOST_ID))
                .isInstanceOf(IllegalStateException.class);
        assertThat(flagJpaRepository.findById(encoreId)).isPresent();
        assertThat(parent().isAutoExpiryExempt()).isTrue();
    }

    private Long encore() {
        LocalDateTime base = LocalDateTime.now().withNano(0);
        Long encoreId = flagHostUseCase.encoreFlag(new FlagEncoreCommand(
                parentId, HOST_ID, base.plusHours(1), base.plusHours(2), base.plusHours(3)));
        createdFlagIds.add(encoreId);
        return encoreId;
    }

    private Flag parent() {
        return flagJpaRepository.findById(parentId).orElseThrow();
    }
}
