package com.example.GooRoomBe.social.application.port.in;

import com.example.GooRoomBe.social.domain.label.Label;
import java.util.List;

public interface LabelCommandUseCase {

    Label createLabel(Long ownerId, String name, boolean exposure);

    void updateLabel(String labelId, Long ownerId, String labelName, Boolean exposure);

    void deleteLabel(Long ownerId, String labelId);

    void replaceLabelMembers(Long currentUserId, String labelId, List<Long> memberIds);

    void addMemberToLabel(Long currentUserId, String labelId, Long memberId);

    void removeMemberFromLabel(Long currentUserId, String labelId, Long memberId);
}
