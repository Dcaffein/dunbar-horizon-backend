package com.example.DunbarHorizon.flag.domain.memorial;

import com.example.DunbarHorizon.flag.domain.flag.exception.FlagInvalidStatusException;
import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagParticipantRepository;
import com.example.DunbarHorizon.flag.domain.memorial.exception.FlagMemorialAuthorizationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FlagMemorialCreator {
    private final FlagParticipantRepository flagParticipantRepository;

    public FlagMemorial create(Flag flag, Long writerId, String content) {
        if(!flag.isEnded()){
            throw new FlagInvalidStatusException("FlagMemorial can be created with ended flag");
        }

        boolean isHost = flag.getHostId().equals(writerId);
        boolean isParticipant = flagParticipantRepository.isParticipating(flag.getId(), writerId);

        if (!isHost && !isParticipant) {
            throw new FlagMemorialAuthorizationException("only flag members can write memorial");
        }

        return new FlagMemorial(flag.getId(), writerId, content);
    }
}
