package com.example.GooRoomBe.flag.application.service.flag;

import com.example.GooRoomBe.flag.domain.flag.Flag;
import com.example.GooRoomBe.flag.domain.flag.repository.FlagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class FlagExpiryService {

    private final FlagRepository flagRepository;

    @Transactional
    public void labelExpiredFlags() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(Flag.EXPIRATION_THRESHOLD_HOURS);

        int count = flagRepository.expireAllExceedingThreshold(threshold);

        if (count > 0) {
            log.info("시스템 자동 만료 처리 완료: {}건의 플래그에 deletedAt 기록", count);
        }
    }
}