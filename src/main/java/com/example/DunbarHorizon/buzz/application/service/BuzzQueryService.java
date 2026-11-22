package com.example.DunbarHorizon.buzz.application.service;

import com.example.DunbarHorizon.buzz.application.dto.info.ReceivedBuzz;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzSummaryResult;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzQueryRepository;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzSocialPort;
import com.example.DunbarHorizon.buzz.application.port.out.ImageStoragePort;
import com.example.DunbarHorizon.buzz.application.service.BuzzProfileAssembler.Profiles;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BuzzQueryService {

    private final BuzzQueryRepository buzzQueryRepository;
    private final BuzzSocialPort buzzSocialPort;
    private final BuzzProfileAssembler buzzProfileAssembler;
    private final ImageStoragePort imageStoragePort;

    public Slice<BuzzSummaryResult> getReceivedBuzzes(Long userId, Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        Set<Long> excludedCreatorIds = buzzSocialPort.getNonReceivableFriendIds(userId);
        Slice<ReceivedBuzz> receivedBuzzes =
                buzzQueryRepository.findReceived(userId, excludedCreatorIds, now, pageable);

        Profiles profiles = buzzProfileAssembler.profilesOf(receivedBuzzes.getContent().stream()
                .map(received -> received.buzz().getCreatorId())
                .toList());
        return receivedBuzzes.map(received -> BuzzSummaryResult.of(
                received.buzz(),
                profiles.of(received.buzz().getCreatorId()),
                imageStoragePort.resolveUrls(received.buzz().getImageKeys()),
                !received.read(),
                false,
                now));
    }

    public Slice<BuzzSummaryResult> getSentBuzzes(Long userId, Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        Slice<Buzz> sentBuzzes = buzzQueryRepository.findSent(userId, now, pageable);

        Profiles profiles = buzzProfileAssembler.profilesOf(List.of(userId));
        return sentBuzzes.map(buzz -> BuzzSummaryResult.of(
                buzz,
                profiles.of(userId),
                imageStoragePort.resolveUrls(buzz.getImageKeys()),
                false,
                true,
                now));
    }

    public List<Long> getUnreadSenderIds(Long userId) {
        Set<Long> excludedCreatorIds = buzzSocialPort.getNonReceivableFriendIds(userId);
        return buzzQueryRepository.findUnreadCreatorIds(userId, excludedCreatorIds, LocalDateTime.now());
    }
}
