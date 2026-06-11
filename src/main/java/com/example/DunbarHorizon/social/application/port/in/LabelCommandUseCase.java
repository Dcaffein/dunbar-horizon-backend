package com.example.DunbarHorizon.social.application.port.in;

import com.example.DunbarHorizon.social.domain.label.Label;
import java.util.List;

public interface LabelCommandUseCase {

    Label createLabel(Long ownerId, String name);

    void updateLabel(String labelId, Long ownerId, String labelName);

    void deleteLabel(Long ownerId, String labelId);

    void replaceLabelMembers(Long currentUserId, String labelId, List<Long> memberIds);

    void addMemberToLabel(Long currentUserId, String labelId, Long memberId);

    void removeMemberFromLabel(Long currentUserId, String labelId, Long memberId);
}
