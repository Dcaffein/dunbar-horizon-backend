package com.example.DunbarHorizon.flag.domain.flag;

import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import com.example.DunbarHorizon.flag.domain.memorial.repository.FlagMemorialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FlagExpiryExemptionUpdater {

    private final FlagRepository flagRepository;
    private final FlagMemorialRepository memorialRepository;

    public void refresh(Flag flag) {
        boolean exempt = memorialRepository.existsByFlagId(flag.getId())
                      || flagRepository.existsByParentId(flag.getId());
        flag.updateAutoExpiryExempt(exempt);
    }
}
