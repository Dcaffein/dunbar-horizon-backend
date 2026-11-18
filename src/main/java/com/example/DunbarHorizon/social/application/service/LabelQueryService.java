package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.application.dto.result.LabelMemberResult;
import com.example.DunbarHorizon.social.application.dto.result.LabelResult;
import com.example.DunbarHorizon.social.application.port.in.LabelQueryUseCase;
import com.example.DunbarHorizon.social.application.port.out.LabelQueryRepository;
import com.example.DunbarHorizon.social.domain.label.exception.LabelAuthorizationException;
import com.example.DunbarHorizon.social.domain.label.exception.LabelNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LabelQueryService implements LabelQueryUseCase {

    private final LabelQueryRepository labelQueryRepository;

    @Override
    public Set<Long> getMemberIdsInOwnedLabels(Long ownerId, List<String> labelIds) {
        return labelQueryRepository.findMemberIdsInOwnedLabels(ownerId, labelIds);
    }

    public List<LabelResult> getAllLabels(Long ownerId) {
        return labelQueryRepository.findAllByOwner(ownerId);
    }

    public List<LabelResult> getLabelsByMember(Long ownerId, Long memberId) {
        return labelQueryRepository.findByOwnerAndMember(ownerId, memberId);
    }

    public LabelResult getLabelById(Long ownerId, String labelId) {
        requireOwner(ownerId, labelId);
        return labelQueryRepository.findById(labelId)
                .orElseThrow(() -> new LabelNotFoundException(labelId));
    }

    public List<LabelMemberResult> getLabelMembers(Long ownerId, String labelId) {
        requireOwner(ownerId, labelId);
        return labelQueryRepository.findMembers(labelId);
    }

    private void requireOwner(Long requesterId, String labelId) {
        Long ownerId = labelQueryRepository.findOwnerId(labelId)
                .orElseThrow(() -> new LabelNotFoundException(labelId));
        if (!ownerId.equals(requesterId)) {
            throw new LabelAuthorizationException(requesterId);
        }
    }
}
