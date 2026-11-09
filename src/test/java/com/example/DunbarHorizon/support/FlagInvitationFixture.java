package com.example.DunbarHorizon.support;

import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.flag.FlagSchedule;
import com.example.DunbarHorizon.flag.domain.invitation.FlagInvitation;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;

public final class FlagInvitationFixture {
    private FlagInvitationFixture() {}

    public static FlagInvitation invitation(Long flagId, Long inviterId, Long inviteeId) {
        LocalDateTime now = LocalDateTime.now();
        Flag flag = Flag.create(inviterId, "초대 테스트", "설명", 10,
                FlagSchedule.of(now.plusHours(1), now.plusHours(2), now.plusHours(3)));
        ReflectionTestUtils.setField(flag, "id", flagId);
        return FlagInvitation.create(flag, inviterId, inviteeId, null, false, false);
    }
}
