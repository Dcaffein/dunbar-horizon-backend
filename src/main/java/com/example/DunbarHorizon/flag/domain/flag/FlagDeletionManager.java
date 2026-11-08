package com.example.DunbarHorizon.flag.domain.flag;

import com.example.DunbarHorizon.flag.domain.flag.exception.FlagNotFoundException;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FlagDeletionManager {

    private final FlagRepository flagRepository;
    private final FlagExpiryExemptionUpdater exemptionUpdater;

    public void delete(Long flagId, Long requesterId) {
        Flag flag = flagRepository.findByIdForUpdate(flagId)
                .orElseThrow(() -> new FlagNotFoundException(flagId));
        Long parentId = flag.getParentId();
        flag.delete(requesterId);
        flagRepository.save(flag);

        if (parentId == null) return;

        flagRepository.findByIdForUpdate(parentId).ifPresent(parent -> {
            exemptionUpdater.refresh(parent);
            flagRepository.save(parent);
        });
    }
}
