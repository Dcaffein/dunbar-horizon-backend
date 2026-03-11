package com.example.DunbarHorizon.flag.domain.flag;

import com.example.DunbarHorizon.flag.domain.flag.exception.FlagInvalidStatusException;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class FlagEncoreCreator {

    private final FlagRepository flagRepository;

    public Flag encore(Flag parentFlag, Long hostId, LocalDateTime deadline, LocalDateTime start, LocalDateTime end) {
        if (!parentFlag.isEnded()) {
            throw new FlagInvalidStatusException("종료된 플래그만 앵코르를 생성할 수 있습니다.");
        }

        if (flagRepository.existsByParentId(parentFlag.getId())) {
            throw new FlagInvalidStatusException("이미 앵콜이 존재하는 플래그입니다.");
        }

        return parentFlag.createEncore(hostId, deadline, start, end);
    }
}
