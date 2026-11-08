package com.example.DunbarHorizon.flag.domain.flag;

import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagAuthorizationException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagInvalidStatusException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagNotFoundException;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import com.example.DunbarHorizon.global.exception.ErrorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class FlagEncoreManager {

    private final FlagRepository flagRepository;
    private final FlagExpiryExemptionUpdater exemptionUpdater;

    public Flag encore(Long parentFlagId, Long hostId, LocalDateTime deadline, LocalDateTime start, LocalDateTime end) {
        Flag parent = flagRepository.findByIdForUpdate(parentFlagId)
                .orElseThrow(() -> new FlagNotFoundException(parentFlagId));

        if (!parent.getHostId().equals(hostId)) {
            throw new FlagAuthorizationException(FlagErrorCode.FLAG_HOST_ONLY,
                    ErrorContext.of("flagId", parentFlagId).and("userId", hostId));
        }
        if (flagRepository.existsByParentId(parentFlagId)) {
            throw new FlagInvalidStatusException(FlagErrorCode.FLAG_ENCORE_ALREADY_EXISTS,
                    ErrorContext.of("flagId", parentFlagId));
        }

        Flag encore = flagRepository.save(parent.createEncore(hostId, deadline, start, end));
        exemptionUpdater.refresh(parent);
        flagRepository.save(parent);
        return encore;
    }
}
