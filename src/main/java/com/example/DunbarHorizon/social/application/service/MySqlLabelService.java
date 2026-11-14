package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.application.dto.SocialUserProfile;
import com.example.DunbarHorizon.social.application.dto.result.LabelMemberResult;
import com.example.DunbarHorizon.social.application.dto.result.LabelResult;
import com.example.DunbarHorizon.social.application.port.in.LabelCommandUseCase;
import com.example.DunbarHorizon.social.application.port.in.LabelQueryUseCase;
import com.example.DunbarHorizon.social.application.port.out.FriendshipCreationRepository;
import com.example.DunbarHorizon.social.application.port.out.LabelMembershipRepository;
import com.example.DunbarHorizon.social.application.port.out.SocialUserProfileReader;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendshipNotFoundException;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import com.example.DunbarHorizon.social.domain.label.Label;
import com.example.DunbarHorizon.social.domain.label.LabelFactory;
import com.example.DunbarHorizon.social.domain.label.exception.LabelAuthorizationException;
import com.example.DunbarHorizon.social.domain.label.exception.LabelNotFoundException;
import com.example.DunbarHorizon.social.domain.label.exception.NonFriendLabelMemberException;
import com.example.DunbarHorizon.social.domain.socialUser.exception.UserReferenceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Transactional(transactionManager = "transactionManager", isolation = Isolation.READ_COMMITTED)
public class MySqlLabelService implements LabelCommandUseCase, LabelQueryUseCase {

    private final LabelMembershipRepository labels;
    private final LabelFactory labelFactory;
    private final FriendshipCreationRepository friendshipLocks;
    private final FriendshipRepository friendships;
    private final SocialUserProfileReader users;

    @Override
    public Label createLabel(Long ownerId, String name) {
        Label label = labelFactory.create(activeProfile(ownerId), name);
        labels.create(label);
        return label;
    }

    @Override
    public void updateLabel(String labelId, Long ownerId, String labelName) {
        requireOwner(labels.lockOwnerId(labelId), labelId, ownerId);
        if (labelName != null) {
            Label.validateName(labelName);
            labels.rename(labelId, labelName);
        }
    }

    @Override
    public void deleteLabel(Long ownerId, String labelId) {
        requireOwner(labels.lockOwnerId(labelId), labelId, ownerId);
        labels.delete(labelId);
    }

    @Override
    public void addMemberToLabel(Long ownerId, String labelId, Long memberId) {
        requireOwner(labels.findOwnerId(labelId), labelId, ownerId);
        if (!friendshipLocks.existsFriendshipForUpdate(ownerId, memberId)) {
            throw new FriendshipNotFoundException(ownerId, memberId);
        }
        activeProfile(memberId);
        requireOwner(labels.lockOwnerId(labelId), labelId, ownerId);
        labels.addMember(labelId, memberId);
    }

    @Override
    public void replaceLabelMembers(Long ownerId, String labelId, List<Long> memberIds) {
        requireOwner(labels.findOwnerId(labelId), labelId, ownerId);
        List<Long> requested = memberIds == null ? List.of() : memberIds.stream().distinct().toList();
        List<String> pairIds = requested.stream().map(id -> Friendship.generateCompositeId(ownerId, id)).toList();
        Set<Long> valid = friendships.findAllByIdsForUpdate(pairIds).stream()
                .map(friendship -> friendship.getFriend(ownerId).getId()).collect(Collectors.toSet());
        List<Long> invalid = requested.stream().filter(id -> !valid.contains(id)).toList();
        if (!invalid.isEmpty()) {
            throw new NonFriendLabelMemberException(invalid);
        }

        requireOwner(labels.lockOwnerId(labelId), labelId, ownerId);
        labels.replaceMembers(labelId, requested);
    }

    @Override
    public void removeMemberFromLabel(Long ownerId, String labelId, Long memberId) {
        requireOwner(labels.lockOwnerId(labelId), labelId, ownerId);
        labels.removeMember(labelId, memberId);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> getMemberIdsByLabels(Long ownerId, List<String> labelIds) {
        return labels.findMemberIdsByOwnerAndLabelIds(ownerId, labelIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabelResult> getAllLabels(Long ownerId) {
        return labels.findAllByOwner_Id(ownerId).stream().map(LabelResult::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabelResult> getLabelsByMember(Long ownerId, Long memberId) {
        return labels.findLabelsByOwnerAndMember(ownerId, memberId).stream().map(LabelResult::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LabelResult getLabelById(Long ownerId, String labelId) {
        return LabelResult.from(ownedLabel(ownerId, labelId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabelMemberResult> getLabelMembers(Long ownerId, String labelId) {
        return ownedLabel(ownerId, labelId).getMembers().stream().map(LabelMemberResult::from).toList();
    }

    private Label ownedLabel(Long ownerId, String labelId) {
        Label label = labels.findById(labelId).orElseThrow(() -> new LabelNotFoundException(labelId));
        requireOwner(Optional.of(label.getOwner().getId()), labelId, ownerId);
        return label;
    }

    private static void requireOwner(Optional<Long> owner, String labelId, Long requesterId) {
        if (!owner.orElseThrow(() -> new LabelNotFoundException(labelId)).equals(requesterId)) {
            throw new LabelAuthorizationException(requesterId);
        }
    }

    private SocialUserProfile activeProfile(Long id) {
        return Optional.ofNullable(users.findActiveProfiles(List.of(id)).get(id))
                .orElseThrow(() -> new UserReferenceNotFoundException(id));
    }
}
