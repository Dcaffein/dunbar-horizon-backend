package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.application.port.out.ActiveUserLookup;
import com.example.DunbarHorizon.social.application.port.out.LabelMemberFriendshipLookup;
import com.example.DunbarHorizon.social.application.port.out.LabelMembershipCleanup;
import com.example.DunbarHorizon.social.application.port.out.LabelRepository;
import com.example.DunbarHorizon.social.domain.label.Label;
import com.example.DunbarHorizon.social.domain.label.LabelMemberRegistrar;
import com.example.DunbarHorizon.social.domain.label.LabelName;
import com.example.DunbarHorizon.social.domain.label.LabelNamer;
import com.example.DunbarHorizon.social.domain.label.exception.LabelAuthorizationException;
import com.example.DunbarHorizon.social.domain.label.exception.LabelNotFoundException;
import com.example.DunbarHorizon.social.domain.socialUser.exception.UserReferenceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class LabelCommandService {

    private final LabelRepository labelRepository;
    private final LabelMembershipCleanup labelMembershipCleanup;
    private final LabelNamer labelNamer;
    private final LabelMemberRegistrar labelMemberRegistrar;
    private final LabelMemberFriendshipLookup labelMemberFriendshipLookup;
    private final ActiveUserLookup activeUserLookup;

    public Label createLabel(Long ownerId, String name) {
        requireActiveUser(ownerId);
        LabelName assignedName = labelNamer.assign(name, labelRepository.findNamesByOwner(ownerId, null));
        Label label = Label.create(ownerId, assignedName);
        labelRepository.save(label);
        return label;
    }

    public void updateLabel(String labelId, Long ownerId, String name) {
        Label label = ownedLabel(ownerId, labelId);
        if (name != null) {
            LabelName assignedName = labelNamer.assign(name, labelRepository.findNamesByOwner(ownerId, labelId));
            label.rename(assignedName);
            labelRepository.save(label);
        }
    }

    public void deleteLabel(Long ownerId, String labelId) {
        Label label = ownedLabel(ownerId, labelId);
        labelRepository.delete(label);
    }

    public void addMemberToLabel(Long ownerId, String labelId, Long memberId) {
        Label label = ownedLabel(ownerId, labelId);
        requireActiveUser(memberId);
        Set<Long> friendIds = labelMemberFriendshipLookup.findFriendIdsWithSharedLock(ownerId, List.of(memberId));
        labelMemberRegistrar.addMember(label, memberId, friendIds);
        labelRepository.save(label);
    }

    public void replaceLabelMembers(Long ownerId, String labelId, List<Long> memberIds) {
        Label label = ownedLabel(ownerId, labelId);
        List<Long> requestedMemberIds = memberIds == null ? List.of() : memberIds.stream().distinct().toList();
        Set<Long> friendIds = labelMemberFriendshipLookup.findFriendIdsWithSharedLock(ownerId, requestedMemberIds);
        labelMemberRegistrar.replaceMembers(label, requestedMemberIds, friendIds);
        labelRepository.save(label);
    }

    public void removeMemberFromLabel(Long ownerId, String labelId, Long memberId) {
        Label label = ownedLabel(ownerId, labelId);
        label.removeMember(memberId);
        labelRepository.save(label);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void removeMembershipsBetween(Long firstUserId, Long secondUserId) {
        labelMembershipCleanup.removeMembershipsBetween(firstUserId, secondUserId);
    }

    private Label ownedLabel(Long ownerId, String labelId) {
        Label label = labelRepository.findById(labelId).orElseThrow(() -> new LabelNotFoundException(labelId));
        if (!label.getOwnerId().equals(ownerId)) {
            throw new LabelAuthorizationException(ownerId);
        }
        return label;
    }

    private void requireActiveUser(Long userId) {
        if (!activeUserLookup.isActive(userId)) {
            throw new UserReferenceNotFoundException(userId);
        }
    }
}
