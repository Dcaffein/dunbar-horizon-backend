package com.example.DunbarHorizon.flag.application.service.flag;

import com.example.DunbarHorizon.flag.application.port.in.FlagParticipationUseCase;
import com.example.DunbarHorizon.flag.domain.flag.FlagParticipant;
import com.example.DunbarHorizon.flag.domain.flag.FlagParticipationManager;
import com.example.DunbarHorizon.flag.domain.flag.FriendshipChecker;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagNotFoundException;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class FlagParticipationService implements FlagParticipationUseCase {
    private final FlagRepository flagRepository;
    private final FlagParticipationManager flagParticipationManager;
    private final FriendshipChecker friendshipChecker;
    private final TransactionTemplate participationTransaction;

    public FlagParticipationService(FlagRepository flagRepository,
                                    FlagParticipationManager flagParticipationManager,
                                    FriendshipChecker friendshipChecker,
                                    PlatformTransactionManager transactionManager) {
        this.flagRepository = flagRepository;
        this.flagParticipationManager = flagParticipationManager;
        this.friendshipChecker = friendshipChecker;
        this.participationTransaction = new TransactionTemplate(transactionManager);
    }

    @Override
    public void participateInFlag(Long flagId, Long userId) {
        Long hostId = flagRepository.findHostIdById(flagId)
                .orElseThrow(() -> new FlagNotFoundException(flagId));
        boolean isHostFriend = friendshipChecker.areFriends(hostId, userId);

        participationTransaction.executeWithoutResult(status -> {
            FlagParticipant newParticipant = flagParticipationManager.participate(flagId, userId, isHostFriend);
            flagRepository.saveParticipant(newParticipant);
        });
    }

    @Override
    @Transactional
    public void leaveFlag(Long flagId, Long userId) {
        FlagParticipant participant = flagParticipationManager.unparticipate(flagId, userId);
        flagRepository.deleteParticipant(participant);
    }
}
