package com.example.DunbarHorizon.flag.application.service.invitation;

import com.example.DunbarHorizon.flag.application.port.out.FlagMaintenancePort;
import com.example.DunbarHorizon.flag.domain.flag.Flag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class FlagInvitationCleanupService {

    private final FlagMaintenancePort maintenancePort;

    @Transactional
    public void purgeDeadInvitations() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(Flag.EXPIRATION_THRESHOLD_HOURS);

        int purged = maintenancePort.purgeInvitationsOfEndedFlags(threshold);

        if (purged > 0) {
            log.info("초대 정리 완료: 종료된 플래그의 초대 {}건 삭제", purged);
        }
    }
}
