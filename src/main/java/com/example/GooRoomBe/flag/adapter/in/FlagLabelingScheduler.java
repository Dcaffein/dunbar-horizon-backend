package com.example.GooRoomBe.flag.adapter.in;

import com.example.GooRoomBe.flag.application.service.flag.FlagExpiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FlagLabelingScheduler {

    private final FlagExpiryService expiryService;

    @Scheduled(cron = "0 0 0/6 * * *")
    public void runLabeling() {
        expiryService.labelExpiredFlags();

    }
}