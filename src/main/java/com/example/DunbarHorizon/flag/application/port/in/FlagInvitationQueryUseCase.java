package com.example.DunbarHorizon.flag.application.port.in;

import com.example.DunbarHorizon.flag.application.dto.result.ReceivedFlagInvitationResult;
import com.example.DunbarHorizon.flag.application.dto.result.SentFlagInvitationResult;

import java.util.List;

public interface FlagInvitationQueryUseCase {
    List<ReceivedFlagInvitationResult> getReceived(Long inviteeId);
    List<SentFlagInvitationResult> getSent(Long inviterId);
}
