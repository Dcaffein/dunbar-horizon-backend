package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.domain.friend.FriendshipDecayPolicy;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FriendshipDecayService {

    private final FriendshipRepository friendshipRepository;
    private final FriendshipDecayPolicy decayPolicy;

    /**
     * 실패를 잡지 않는다. 스케줄러의 에러 핸들러가 ERROR로 남기고, 다음 트리거가 같은 대상을 다시 훑는다.
     */
    public void processDecay() {
        double rate = decayPolicy.getDecayRate();
        double minThreshold = decayPolicy.getMinThreshold();
        LocalDateTime thresholdTime = decayPolicy.getDecayThresholdTime();

        friendshipRepository.applyDecay(rate, minThreshold, thresholdTime);
    }
}
