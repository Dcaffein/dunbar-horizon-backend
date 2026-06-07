package com.example.DunbarHorizon.flag.domain.memorial;

import com.example.DunbarHorizon.flag.domain.flag.exception.FlagInvalidStatusException;
import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import com.example.DunbarHorizon.flag.domain.memorial.exception.FlagMemorialAuthorizationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FlagMemorialFactory {
    private final FlagRepository flagRepository;

    public FlagMemorial create(Flag flag, Long writerId, String content) {
        if(!flag.isEnded()){
            throw new FlagInvalidStatusException("종료된 플래그에만 후기를 작성할 수 있습니다.");
        }

        boolean isHost = flag.getHostId().equals(writerId);
        boolean isParticipant = flagRepository.isParticipating(flag.getId(), writerId);

        if (!isHost && !isParticipant) {
            throw new FlagMemorialAuthorizationException("플래그 참여자만 후기를 작성할 수 있습니다.");
        }

        return new FlagMemorial(flag.getId(), writerId, content);
    }
}
