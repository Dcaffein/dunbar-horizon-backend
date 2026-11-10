package com.example.DunbarHorizon.flag.application.service.flag;

import com.example.DunbarHorizon.flag.adapter.out.persistence.jpa.FlagJpaRepository;
import com.example.DunbarHorizon.flag.application.port.in.FlagHostUseCase;
import com.example.DunbarHorizon.flag.application.port.in.FlagModificationUseCase;
import com.example.DunbarHorizon.flag.application.port.in.command.FlagEncoreCommand;
import com.example.DunbarHorizon.flag.application.port.in.command.FlagHostCommand;
import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.support.TestContainerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 앙코르 유일 제약과 부모 링크 끊기는 실제 커밋으로만 확인할 수 있어
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
    @DisplayName("앙코르를 삭제한 뒤 같은 부모로 앙코르를 다시 만들 수 있고, 부모의 보존 필드는 바뀌지 않는다")
    void encore_Succeeds_AfterPreviousEncoreDeleted() {
        // given
        Long deletedEncoreId = encore();
        flagModificationUseCase.closeFlag(deletedEncoreId, HOST_ID);

        // when
        Long recreatedEncoreId = encore();

        // then
        assertThat(flagJpaRepository.findById(recreatedEncoreId).orElseThrow().getParentId()).isEqualTo(parentId);
        assertThat(parent().isPreserved()).isFalse();
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
