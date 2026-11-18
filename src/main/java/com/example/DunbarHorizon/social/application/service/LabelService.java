package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.application.dto.result.LabelMemberResult;
import com.example.DunbarHorizon.social.application.dto.result.LabelResult;
import com.example.DunbarHorizon.social.application.port.in.LabelQueryUseCase;
import com.example.DunbarHorizon.social.application.port.out.FriendshipLockRepository;
import com.example.DunbarHorizon.social.application.port.out.LabelQueryRepository;
import com.example.DunbarHorizon.social.application.port.out.LabelRepository;
import com.example.DunbarHorizon.social.application.port.out.UserProfileRepository;
import com.example.DunbarHorizon.social.domain.label.Label;
import com.example.DunbarHorizon.social.domain.label.LabelMemberRegistrar;
import com.example.DunbarHorizon.social.domain.label.LabelNamer;
import com.example.DunbarHorizon.social.domain.label.exception.LabelAuthorizationException;
import com.example.DunbarHorizon.social.domain.label.exception.LabelNotFoundException;
import com.example.DunbarHorizon.social.domain.socialUser.exception.UserReferenceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(isolation = Isolation.READ_COMMITTED)
public class LabelService implements LabelQueryUseCase {

    private final LabelRepository labelRepository;
    private final LabelQueryRepository labelQueryRepository;
    private final LabelNamer labelNamer;
    private final LabelMemberRegistrar labelMemberRegistrar;
    private final FriendshipLockRepository friendshipLockRepository;
    private final UserProfileRepository userProfileRepository;

    public Label createLabel(Long ownerId, String name) {
        requireActiveProfile(ownerId);
        boolean nameExists = labelRepository.existsByOwnerAndName(ownerId, name);
        Label label = labelNamer.create(ownerId, name, nameExists);
        labelRepository.create(label);
        return label;
    }

    public void updateLabel(String labelId, Long ownerId, String name) {
        Label label = ownedLabel(ownerId, labelId);
        requireOwner(labelRepository.lockOwnerId(labelId), labelId, ownerId);
        if (name != null) {
            boolean nameExists = labelRepository.existsByOwnerAndNameExcludingLabel(ownerId, name, labelId);
            labelNamer.rename(label, name, nameExists);
            labelRepository.saveChanges(label);
        }
    }

    public void deleteLabel(Long ownerId, String labelId) {
        Label label = ownedLabel(ownerId, labelId);
        requireOwner(labelRepository.lockOwnerId(labelId), labelId, ownerId);
        labelRepository.delete(label);
    }

    public void addMemberToLabel(Long ownerId, String labelId, Long memberId) {
        Label label = ownedLabel(ownerId, labelId);
        requireActiveProfile(memberId);
        Set<Long> friendIds = friendshipLockRepository.existsFriendshipForUpdate(ownerId, memberId)
                ? Set.of(memberId) : Set.of();
        requireOwner(labelRepository.lockOwnerId(labelId), labelId, ownerId);
        labelMemberRegistrar.addMember(label, memberId, friendIds);
        labelRepository.saveChanges(label);
    }

    public void replaceLabelMembers(Long ownerId, String labelId, List<Long> memberIds) {
        Label label = ownedLabel(ownerId, labelId);
        List<Long> requested = memberIds == null ? List.of() : memberIds.stream().distinct().toList();
        Set<Long> friendIds = friendshipLockRepository.findAllBetweenForUpdate(ownerId, requested).stream()
                .map(friendship -> friendship.getFriendId(ownerId)).collect(Collectors.toSet());
        requireOwner(labelRepository.lockOwnerId(labelId), labelId, ownerId);
        labelMemberRegistrar.replaceMembers(label, requested, friendIds);
        labelRepository.saveChanges(label);
    }

    public void removeMemberFromLabel(Long ownerId, String labelId, Long memberId) {
        Label label = ownedLabel(ownerId, labelId);
        requireOwner(labelRepository.lockOwnerId(labelId), labelId, ownerId);
        label.removeMember(memberId);
        labelRepository.saveChanges(label);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> getMemberIdsByLabels(Long ownerId, List<String> labelIds) {
        return labelQueryRepository.findMemberIdsByOwnerAndLabelIds(ownerId, labelIds);
    }

    @Transactional(readOnly = true)
    public List<LabelResult> getAllLabels(Long ownerId) {
        return labelQueryRepository.findAllByOwner(ownerId);
    }

    @Transactional(readOnly = true)
    public List<LabelResult> getLabelsByMember(Long ownerId, Long memberId) {
        return labelQueryRepository.findByOwnerAndMember(ownerId, memberId);
    }

    @Transactional(readOnly = true)
    public LabelResult getLabelById(Long ownerId, String labelId) {
        requireOwner(labelRepository.findOwnerId(labelId), labelId, ownerId);
        return labelQueryRepository.findResultById(labelId)
                .orElseThrow(() -> new LabelNotFoundException(labelId));
    }

    @Transactional(readOnly = true)
    public List<LabelMemberResult> getLabelMembers(Long ownerId, String labelId) {
        requireOwner(labelRepository.findOwnerId(labelId), labelId, ownerId);
        return labelQueryRepository.findMembers(labelId);
    }

    private Label ownedLabel(Long ownerId, String labelId) {
        Label label = labelRepository.findById(labelId).orElseThrow(() -> new LabelNotFoundException(labelId));
        requireOwner(Optional.of(label.getOwnerId()), labelId, ownerId);
        return label;
    }

    private void requireOwner(Optional<Long> owner, String labelId, Long requesterId) {
        if (!owner.orElseThrow(() -> new LabelNotFoundException(labelId)).equals(requesterId)) {
            throw new LabelAuthorizationException(requesterId);
        }
    }

    private void requireActiveProfile(Long id) {
        if (!userProfileRepository.findActiveProfiles(List.of(id)).containsKey(id)) {
            throw new UserReferenceNotFoundException(id);
        }
    }
}
