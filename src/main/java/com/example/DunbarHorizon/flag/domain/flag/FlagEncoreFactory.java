package com.example.DunbarHorizon.flag.domain.flag;



import com.example.DunbarHorizon.global.exception.ErrorContext;import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;import com.example.DunbarHorizon.flag.domain.flag.exception.FlagAuthorizationException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagInvalidStatusException;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class FlagEncoreFactory {

    private final FlagRepository flagRepository;

    public Flag encore(Flag parentFlag, Long hostId, LocalDateTime deadline, LocalDateTime start, LocalDateTime end) {
        if (!parentFlag.getHostId().equals(hostId)) {
            throw new FlagAuthorizationException(FlagErrorCode.FLAG_HOST_ONLY, ErrorContext.of("flagId", parentFlag.getId()).and("userId", hostId));
        }

        if (flagRepository.existsByParentId(parentFlag.getId())) {
            throw new FlagInvalidStatusException(FlagErrorCode.FLAG_ENCORE_ALREADY_EXISTS, ErrorContext.of("flagId", parentFlag.getId()));
        }

        return parentFlag.createEncore(hostId, deadline, start, end);
    }
}
