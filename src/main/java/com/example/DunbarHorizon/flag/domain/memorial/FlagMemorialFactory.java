package com.example.DunbarHorizon.flag.domain.memorial;



import com.example.DunbarHorizon.global.exception.ErrorContext;import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;import com.example.DunbarHorizon.flag.domain.flag.exception.FlagInvalidStatusException;
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
            throw new FlagInvalidStatusException(FlagErrorCode.FLAG_NOT_ENDED, ErrorContext.of("flagId", flag.getId()));
        }

        boolean isHost = flag.getHostId().equals(writerId);
        boolean isParticipant = flagRepository.isParticipating(flag.getId(), writerId);

        if (!isHost && !isParticipant) {
            throw new FlagMemorialAuthorizationException(FlagErrorCode.FLAG_MEMORIAL_PARTICIPANT_ONLY,
                    ErrorContext.of("flagId", flag.getId()).and("userId", writerId));
        }

        return new FlagMemorial(flag.getId(), writerId, content);
    }
}
