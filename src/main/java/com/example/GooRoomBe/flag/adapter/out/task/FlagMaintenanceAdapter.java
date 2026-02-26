package com.example.GooRoomBe.flag.adapter.out.task;

import com.example.GooRoomBe.flag.adapter.out.persistence.jpa.FlagCommentJpaRepository;
import com.example.GooRoomBe.flag.adapter.out.persistence.jpa.FlagJpaRepository;
import com.example.GooRoomBe.flag.adapter.out.persistence.jpa.FlagMemorialJpaRepository;
import com.example.GooRoomBe.flag.adapter.out.persistence.jpa.FlagParticipantJpaRepository;
import com.example.GooRoomBe.flag.application.port.out.FlagMaintenancePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlagMaintenanceAdapter implements FlagMaintenancePort {

    private final FlagJpaRepository flagJpaRepository;
    private final FlagParticipantJpaRepository participantRepositoryAdapter;
    private final FlagMemorialJpaRepository memorialRepositoryAdapter;
    private final FlagCommentJpaRepository commentRepositoryAdapter;
    private final TransactionTemplate transactionTemplate;

    @Override
    public List<Long> findIdsReadyForHardDelete(LocalDateTime bufferTime) {
        return flagJpaRepository.findIdsByDeletedAtBefore(bufferTime);
    }

    @Override
    public void purgeFlagsAndRelatedData(Collection<Long> flagIds) {
        List<Long> idList = new ArrayList<>(flagIds);
        int chunkSize = 500;

        for (int i = 0; i < idList.size(); i += chunkSize) {
            int end = Math.min(i + chunkSize, idList.size());
            List<Long> chunk = idList.subList(i, end);

            transactionTemplate.execute(status -> {
                participantRepositoryAdapter.hardDeleteByFlagIdsIn(chunk);
                memorialRepositoryAdapter.hardDeleteByFlagIdsIn(chunk);
                commentRepositoryAdapter.hardDeleteByFlagIdsIn(chunk);
                flagJpaRepository.hardDeleteByIdsIn(chunk);
                return null;
            });

            log.debug("물리 삭제 진행 중: {}건 완료", end);
        }
    }
}
