package com.example.DunbarHorizon.flag.application.service.invitation;

import com.example.DunbarHorizon.flag.application.port.out.FlagMaintenancePort;
import com.example.DunbarHorizon.flag.domain.flag.Flag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FlagInvitationCleanupServiceTest {

    @InjectMocks private FlagInvitationCleanupService cleanupService;
    @Mock private FlagMaintenancePort maintenancePort;

    @Test
    @DisplayName("종료 24시간이 지난 플래그의 초대를 지운다")
    void purgeDeadInvitations_UsesExpirationThreshold() {
        // given
        LocalDateTime expected = LocalDateTime.now().minusHours(Flag.EXPIRATION_THRESHOLD_HOURS);

        // when
        cleanupService.purgeDeadInvitations();

        // then
        ArgumentCaptor<LocalDateTime> threshold = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(maintenancePort).purgeInvitationsOfEndedFlags(threshold.capture());
        assertThat(threshold.getValue()).isBetween(expected.minusSeconds(1), expected.plusSeconds(1));
    }
}
