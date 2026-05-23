package com.example.DunbarHorizon.flag.application.service.flag;

import com.example.DunbarHorizon.flag.application.port.in.FlagParticipationUseCase;
import com.example.DunbarHorizon.flag.domain.flag.DeletableParticipant;
import com.example.DunbarHorizon.flag.domain.flag.FlagParticipant;
import com.example.DunbarHorizon.flag.domain.flag.FlagParticipationPolicy;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FlagParticipationService implements FlagParticipationUseCase {
    private final FlagParticipantRepository participantRepository;
    private final FlagParticipationPolicy flagParticipationPolicy;

    @Override
    public void participateInFlag(Long flagId, Long userId) {
        FlagParticipant newParticipant = flagParticipationPolicy.participate(flagId, userId);
        participantRepository.save(newParticipant);
    }

    @Override
    public void leaveFlag(Long flagId, Long userId) {
        DeletableParticipant ticket = flagParticipationPolicy.unparticipate(flagId, userId);
        participantRepository.delete(ticket);
    }
}
