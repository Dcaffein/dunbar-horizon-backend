package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.domain.socialUser.UserReference;
import com.example.DunbarHorizon.social.application.port.in.LabelCommandUseCase;
import com.example.DunbarHorizon.social.application.port.in.LabelQueryUseCase;
import com.example.DunbarHorizon.social.application.dto.result.LabelResult;
import com.example.DunbarHorizon.social.domain.label.exception.LabelAuthorizationException;
import com.example.DunbarHorizon.social.domain.label.exception.LabelNotFoundException;
import com.example.DunbarHorizon.social.domain.label.Label;
import com.example.DunbarHorizon.social.domain.label.LabelCreator;
import com.example.DunbarHorizon.social.domain.label.LabelMemberRegistry;
import com.example.DunbarHorizon.social.domain.label.LabelNamePolicy;
import com.example.DunbarHorizon.social.domain.label.repository.LabelRepository;
import com.example.DunbarHorizon.social.domain.socialUser.repository.SocialUserRepository;
import com.example.DunbarHorizon.social.domain.socialUser.exception.UserReferenceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class LabelService implements LabelCommandUseCase, LabelQueryUseCase {
    private final LabelRepository labelRepository;
    private final SocialUserRepository socialUserRepository;
    private final LabelNamePolicy labelNamePolicy;
    private final LabelMemberRegistry labelMemberRegistry;
    private final LabelCreator labelCreator;

    @Transactional
    public Label createLabel(Long currentUserId, String labelName, boolean exposure) {
        labelNamePolicy.validateLabelNameUniqueness(currentUserId, labelName);
        UserReference owner = socialUserRepository.findById(currentUserId)
                .orElseThrow(() -> new UserReferenceNotFoundException(currentUserId));
        return labelRepository.save(labelCreator.create(owner, labelName, exposure));
    }

    @Transactional
    public void deleteLabel(Long currentUserId, String labelId) {
        Label label = getLabel(labelId);
        if (label.getOwner().getId().equals(currentUserId)) {
            labelRepository.delete(label);
        }
    }

    @Transactional
    public void addMemberToLabel(Long currentUserId, String labelId, Long newMemberId) {
        Label label = getLabel(labelId);
        validateOwner(label, currentUserId);
        UserReference newMember = socialUserRepository.findById(newMemberId)
                .orElseThrow(() -> new UserReferenceNotFoundException(newMemberId));
        labelMemberRegistry.addNewMember(label, newMember);
        labelRepository.save(label);
    }

    @Transactional
    public void removeMemberFromLabel(Long currentUserId, String labelId, Long memberIdToRemove) {
        Label label = getLabel(labelId);
        validateOwner(label, currentUserId);
        UserReference memberToRemove = socialUserRepository.findById(memberIdToRemove)
                .orElseThrow(() -> new UserReferenceNotFoundException(memberIdToRemove));
        label.removeMember(memberToRemove);
        labelRepository.save(label);
    }

    @Transactional
    public void replaceLabelMembers(Long currentUserId, String labelId, List<Long> potentialMemberIds) {
        Label label = getLabel(labelId);
        validateOwner(label, currentUserId);
        labelMemberRegistry.updateMembers(label, potentialMemberIds);
        labelRepository.save(label);
    }

    @Transactional
    public void updateLabel(String labelId, Long currentUserId, String labelName, Boolean exposure) {
        Label label = getLabel(labelId);

        applyIfPresent(labelName, newLabelName -> labelNamePolicy.changeLabelName(label, newLabelName));
        applyIfPresent(exposure, newExposureStatus -> label.updateExposure(currentUserId, newExposureStatus));

        labelRepository.save(label);
    }

    @Override
    public Set<Long> getMemberIdsByLabels(Long ownerId, List<String> labelIds) {
        return labelRepository.findMemberIdsByOwnerAndLabelIds(ownerId, labelIds);
    }

    @Override
    public List<LabelResult> getAllLabels(Long ownerId) {
        return labelRepository.findAllByOwner_Id(ownerId).stream()
                .map(LabelResult::from)
                .toList();
    }

    private void validateOwner(Label label, Long currentUserId) {
        if (!label.getOwner().getId().equals(currentUserId)) {
            throw new LabelAuthorizationException(currentUserId);
        }
    }

    private <T> void applyIfPresent(T value, Consumer<T> action) {
        if (value != null) {
            action.accept(value);
        }
    }

    private Label getLabel(String labelId) {
        return labelRepository.findById(labelId).orElseThrow(() -> new LabelNotFoundException(labelId));
    }
}
