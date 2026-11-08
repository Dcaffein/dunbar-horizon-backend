package com.example.DunbarHorizon.flag.domain.memorial;

import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;
import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.flag.FlagExpiryExemptionUpdater;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagInvalidStatusException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagNotFoundException;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import com.example.DunbarHorizon.flag.domain.memorial.exception.FlagMemorialAuthorizationException;
import com.example.DunbarHorizon.flag.domain.memorial.exception.FlagMemorialNotFoundException;
import com.example.DunbarHorizon.flag.domain.memorial.repository.FlagMemorialRepository;
import com.example.DunbarHorizon.global.exception.ErrorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FlagMemorialManager {

    private final FlagRepository flagRepository;
    private final FlagMemorialRepository memorialRepository;
    private final FlagExpiryExemptionUpdater exemptionUpdater;

    public FlagMemorial writeMemorial(Long flagId, Long writerId, String content) {
        Flag flag = lock(flagId);
        if (!flag.isEnded()) {
            throw new FlagInvalidStatusException(FlagErrorCode.FLAG_NOT_ENDED, ErrorContext.of("flagId", flagId));
        }
        if (!flag.getHostId().equals(writerId) && !flagRepository.isParticipating(flagId, writerId)) {
            throw new FlagMemorialAuthorizationException(FlagErrorCode.FLAG_MEMORIAL_PARTICIPANT_ONLY,
                    ErrorContext.of("flagId", flagId).and("userId", writerId));
        }

        FlagMemorial memorial = memorialRepository.save(new FlagMemorial(flagId, writerId, content));
        refreshExemption(flag);
        return memorial;
    }

    public void deleteMemorial(Long flagId, Long memorialId, Long requesterId) {
        Flag flag = lock(flagId);
        FlagMemorial memorial = memorialRepository.findById(memorialId)
                .filter(found -> flagId.equals(found.getFlagId()))
                .orElseThrow(() -> new FlagMemorialNotFoundException(memorialId));
        memorial.validateDeletion(requesterId);
        memorialRepository.delete(memorial);
        refreshExemption(flag);
    }

    private Flag lock(Long flagId) {
        return flagRepository.findByIdForUpdate(flagId)
                .orElseThrow(() -> new FlagNotFoundException(flagId));
    }

    private void refreshExemption(Flag flag) {
        exemptionUpdater.refresh(flag);
        flagRepository.save(flag);
    }
}
