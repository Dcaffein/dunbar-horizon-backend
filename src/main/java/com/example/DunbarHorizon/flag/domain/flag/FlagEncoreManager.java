package com.example.DunbarHorizon.flag.domain.flag;

import com.example.DunbarHorizon.flag.domain.flag.exception.FlagNotFoundException;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class FlagEncoreManager {

    private final FlagRepository flagRepository;
    private final FlagEncoreFactory encoreFactory;
    private final FlagExpiryExemptionUpdater exemptionUpdater;

    public Flag encore(Long parentFlagId, Long hostId, LocalDateTime deadline, LocalDateTime start, LocalDateTime end) {
        Flag parent = flagRepository.findByIdForUpdate(parentFlagId)
                .orElseThrow(() -> new FlagNotFoundException(parentFlagId));
        Flag encore = flagRepository.save(encoreFactory.encore(parent, hostId, deadline, start, end));
        exemptionUpdater.refresh(parent);
        flagRepository.save(parent);
        return encore;
    }
}
