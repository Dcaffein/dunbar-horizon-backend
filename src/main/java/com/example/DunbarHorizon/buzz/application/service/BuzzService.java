package com.example.DunbarHorizon.buzz.application.service;

import com.example.DunbarHorizon.buzz.application.dto.command.CreateBuzzCommand;
import com.example.DunbarHorizon.buzz.application.dto.command.UpdateBuzzCommand;
import com.example.DunbarHorizon.buzz.application.dto.info.recipient.AnchorRecipientSpec;
import com.example.DunbarHorizon.buzz.application.dto.info.recipient.LabelRecipientSpec;
import com.example.DunbarHorizon.buzz.application.dto.info.recipient.ManualRecipientSpec;
import com.example.DunbarHorizon.buzz.application.dto.info.recipient.RecipientSpec;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzSocialPort;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.BuzzAudienceMember;
import com.example.DunbarHorizon.buzz.domain.event.BuzzCreatedEvent;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzNotFoundException;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzAudienceMemberRepository;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class BuzzService {

    private final BuzzRepository buzzRepository;
    private final BuzzAudienceMemberRepository buzzAudienceMemberRepository;
    private final BuzzSocialPort buzzSocialPort;
    private final ApplicationEventPublisher applicationEventPublisher;

    public void createBuzz(CreateBuzzCommand command, List<String> imageKeys) {
        Set<Long> audienceIds = audienceIdsOf(command.creatorId(), command.spec());

        Buzz buzz = Buzz.create(command.creatorId(), command.text(), imageKeys, audienceIds, LocalDateTime.now());
        buzzRepository.save(buzz);
        buzzAudienceMemberRepository.saveAll(audienceIds.stream()
                .map(userId -> BuzzAudienceMember.create(buzz.getId(), userId))
                .toList());

        applicationEventPublisher.publishEvent(new BuzzCreatedEvent(buzz.getId(), command.creatorId(), audienceIds));
    }

    public void updateBuzz(UpdateBuzzCommand command) {
        Buzz buzz = buzzRepository.findById(command.buzzId())
                .orElseThrow(() -> new BuzzNotFoundException(command.buzzId()));
        buzz.update(command.requesterId(), command.text(), command.imageKeys(), LocalDateTime.now());
    }

    public void readBuzz(Long readerId, Long buzzId) {
        Buzz buzz = buzzRepository.findByIdForShare(buzzId)
                .orElseThrow(() -> new BuzzNotFoundException(buzzId));
        Optional<BuzzAudienceMember> member = buzz.isCreator(readerId)
                ? Optional.empty()
                : buzzAudienceMemberRepository.findByBuzzIdAndUserId(buzzId, readerId);
        buzz.validateAccess(readerId, member.isPresent());

        member.ifPresent(BuzzAudienceMember::read);
    }

    public void deleteBuzz(Long requesterId, Long buzzId) {
        Buzz buzz = buzzRepository.findByIdForUpdate(buzzId)
                .orElseThrow(() -> new BuzzNotFoundException(buzzId));
        buzz.delete(requesterId, LocalDateTime.now());
    }

    private Set<Long> audienceIdsOf(Long creatorId, RecipientSpec spec) {
        return switch (spec) {
            case ManualRecipientSpec(List<Long> memberIds) -> buzzSocialPort.getFriendIdsIn(creatorId,
                    memberIds.stream().filter(memberId -> !memberId.equals(creatorId)).toList());
            case LabelRecipientSpec(List<String> labelIds) -> buzzSocialPort.getLabelMemberIds(creatorId, labelIds);
            case AnchorRecipientSpec(Long anchorFriendId, Double expansionValue) ->
                    buzzSocialPort.getAnchorExpansionIds(creatorId, anchorFriendId, expansionValue);
        };
    }
}
