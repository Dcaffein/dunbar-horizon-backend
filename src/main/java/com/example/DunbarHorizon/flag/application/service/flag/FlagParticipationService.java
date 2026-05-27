package com.example.DunbarHorizon.flag.application.service.flag;

import com.example.DunbarHorizon.flag.application.port.in.FlagParticipationUseCase;
import com.example.DunbarHorizon.flag.domain.flag.FlagParticipant;
import com.example.DunbarHorizon.flag.domain.flag.FlagParticipationManager;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FlagParticipationService implements FlagParticipationUseCase {
    private final FlagParticipantRepository participantRepository;
    private final FlagParticipationManager flagParticipationManager;

    @Override
    public void participateInFlag(Long flagId, Long userId) {
        FlagParticipant newParticipant = flagParticipationManager.participate(flagId, userId);
        participantRepository.save(newParticipant);
    }

    @Override
    public void leaveFlag(Long flagId, Long userId) {
        FlagParticipant participant = flagParticipationManager.unparticipate(flagId, userId);
        participantRepository.delete(participant);
    }
}
