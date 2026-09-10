package com.example.DunbarHorizon.flag.application.service.flag;

import com.example.DunbarHorizon.flag.application.port.in.FlagParticipationUseCase;
import com.example.DunbarHorizon.flag.domain.flag.FlagParticipant;
import com.example.DunbarHorizon.flag.domain.flag.FlagParticipationManager;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FlagParticipationService implements FlagParticipationUseCase {
    private final FlagRepository flagRepository;
    private final FlagParticipationManager flagParticipationManager;

    @Override
    // Flag 행을 잠근 뒤 countParticipants가 먼저 커밋한 참여자를 읽어야 정원을 지킬 수 있다.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void participateInFlag(Long flagId, Long userId) {
        FlagParticipant newParticipant = flagParticipationManager.participate(flagId, userId);
        flagRepository.saveParticipant(newParticipant);
    }

    @Override
    public void leaveFlag(Long flagId, Long userId) {
        FlagParticipant participant = flagParticipationManager.unparticipate(flagId, userId);
        flagRepository.deleteParticipant(participant);
    }
}
