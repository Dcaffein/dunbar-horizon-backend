package com.example.DunbarHorizon.flag.domain.flag;

import com.example.DunbarHorizon.flag.domain.flag.event.FlagConclusionKind;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import com.example.DunbarHorizon.flag.domain.memorial.repository.FlagMemorialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class FlagConclusionDecider {

    private final FlagRepository flagRepository;
    private final FlagMemorialRepository memorialRepository;

    public FlagConclusionKind conclude(Flag flag, LocalDateTime now) {
        boolean leftTrace = memorialRepository.existsByFlagId(flag.getId())
                         || flagRepository.existsByParentId(flag.getId());
        if (leftTrace) {
            flag.preserve();
            return FlagConclusionKind.PRESERVED;
        }
        flag.expire(now);
        return FlagConclusionKind.ENDED;
    }
}
