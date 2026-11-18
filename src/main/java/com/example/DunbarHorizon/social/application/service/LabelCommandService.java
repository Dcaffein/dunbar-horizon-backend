package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.application.port.out.ActiveUserRepository;
import com.example.DunbarHorizon.social.application.port.out.FriendshipLockRepository;
import com.example.DunbarHorizon.social.application.port.out.LabelMembershipRepository;
import com.example.DunbarHorizon.social.application.port.out.LabelRepository;
import com.example.DunbarHorizon.social.domain.label.Label;
import com.example.DunbarHorizon.social.domain.label.LabelMemberRegistrar;
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
    private final LabelMembershipRepository labelMembershipRepository;
    private final LabelNamer labelNamer;
    private final LabelMemberRegistrar labelMemberRegistrar;
    private final FriendshipLockRepository friendshipLockRepository;
    private final ActiveUserRepository activeUserRepository;

    public Label createLabel(Long ownerId, String name) {
        requireActiveUser(ownerId);
        boolean nameExists = labelRepository.existsByOwnerAndName(ownerId, name);
        Label label = labelNamer.create(ownerId, name, nameExists);
        labelRepository.create(label);
        return label;
    }

    public void updateLabel(String labelId, Long ownerId, String name) {
        Label label = ownedLabel(ownerId, labelId);
        if (name != null) {
            boolean nameExists = labelRepository.existsByOwnerAndNameExcludingLabel(ownerId, name, labelId);
            labelNamer.rename(label, name, nameExists);
            labelRepository.saveChanges(label);
        }
    }

    public void deleteLabel(Long ownerId, String labelId) {
        Label label = ownedLabel(ownerId, labelId);
        labelRepository.delete(label);
    }

    public void addMemberToLabel(Long ownerId, String labelId, Long memberId) {
        Label label = ownedLabel(ownerId, labelId);
        requireActiveUser(memberId);
        Set<Long> friendIds = friendshipLockRepository.findFriendIdsForShare(ownerId, List.of(memberId));
        labelMemberRegistrar.addMember(label, memberId, friendIds);
        labelRepository.saveChanges(label);
    }

    public void replaceLabelMembers(Long ownerId, String labelId, List<Long> memberIds) {
        Label label = ownedLabel(ownerId, labelId);
        List<Long> requestedMemberIds = memberIds == null ? List.of() : memberIds.stream().distinct().toList();
        Set<Long> friendIds = friendshipLockRepository.findFriendIdsForShare(ownerId, requestedMemberIds);
        labelMemberRegistrar.replaceMembers(label, requestedMemberIds, friendIds);
        labelRepository.saveChanges(label);
    }

    public void removeMemberFromLabel(Long ownerId, String labelId, Long memberId) {
        Label label = ownedLabel(ownerId, labelId);
        label.removeMember(memberId);
        labelRepository.saveChanges(label);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void removeMembersBetween(Long firstUserId, Long secondUserId) {
        labelMembershipRepository.removeMembersBetween(firstUserId, secondUserId);
    }

    private Label ownedLabel(Long ownerId, String labelId) {
        Label label = labelRepository.findById(labelId).orElseThrow(() -> new LabelNotFoundException(labelId));
        if (!label.getOwnerId().equals(ownerId)) {
            throw new LabelAuthorizationException(ownerId);
        }
        return label;
    }

    private void requireActiveUser(Long userId) {
        if (!activeUserRepository.existsById(userId)) {
            throw new UserReferenceNotFoundException(userId);
        }
    }
}
