package com.example.DunbarHorizon.flag.application.service.flag;

import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.flag.FlagConclusionDecider;
import com.example.DunbarHorizon.flag.domain.flag.event.FlagConcludedEvent;
import com.example.DunbarHorizon.flag.domain.flag.event.FlagConclusionKind;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class FlagExpiryService {

    private final FlagRepository flagRepository;
    private final FlagConclusionDecider conclusionDecider;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void concludeEndedFlags() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.minusHours(Flag.EXPIRATION_THRESHOLD_HOURS);

        List<Flag> candidates = flagRepository.findConclusionCandidates(threshold);
        Map<Long, List<Long>> participantsByFlagId = flagRepository.findAllParticipantIdsByFlagIds(
                candidates.stream().map(Flag::getId).toList());

        int preserved = 0;
        for (Flag flag : candidates) {
            FlagConclusionKind kind = conclusionDecider.conclude(flag, now);
            flagRepository.save(flag);
            publishConclusion(flag, kind, participantsByFlagId.get(flag.getId()));
            if (kind == FlagConclusionKind.PRESERVED) preserved++;
        }

        if (!candidates.isEmpty()) {
            log.info("자동 만료 완료: 플래그 {}건 보존, {}건 소프트 삭제",
                    preserved, candidates.size() - preserved);
        }
    }

    private void publishConclusion(Flag flag, FlagConclusionKind kind, List<Long> participantIds) {
        if (participantIds == null || participantIds.isEmpty()) return;

        eventPublisher.publishEvent(new FlagConcludedEvent(
                flag.getId(), flag.getHostId(), flag.getParentId(), participantIds, kind));
    }
}
