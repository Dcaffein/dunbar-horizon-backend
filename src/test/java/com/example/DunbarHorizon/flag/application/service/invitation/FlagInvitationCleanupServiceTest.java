package com.example.DunbarHorizon.flag.application.service.invitation;

import com.example.DunbarHorizon.flag.application.port.out.FlagMaintenancePort;
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
    @DisplayName("현재 시각을 기준으로 시작한 플래그의 초대를 지운다")
    void purgeDeadInvitations_UsesNowAsReference() {
        // given
        LocalDateTime expected = LocalDateTime.now();

        // when
        cleanupService.purgeDeadInvitations();

        // then
        ArgumentCaptor<LocalDateTime> threshold = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(maintenancePort).purgeInvitationsOfStartedFlags(threshold.capture());
        assertThat(threshold.getValue()).isBetween(expected.minusSeconds(1), expected.plusSeconds(1));
    }
}
