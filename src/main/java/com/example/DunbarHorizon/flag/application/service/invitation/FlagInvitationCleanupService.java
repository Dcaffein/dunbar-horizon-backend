package com.example.DunbarHorizon.flag.application.service.invitation;

import com.example.DunbarHorizon.flag.application.port.out.FlagMaintenancePort;
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
        int purged = maintenancePort.purgeInvitationsOfStartedFlags(LocalDateTime.now());

        if (purged > 0) {
            log.info("초대 정리 완료: 시작한 플래그의 초대 {}건 삭제", purged);
        }
    }
}
