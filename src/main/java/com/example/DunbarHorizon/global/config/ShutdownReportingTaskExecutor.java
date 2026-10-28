package com.example.DunbarHorizon.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 종료 대기가 끝난 뒤에도 큐에 남아 있는 작업 수를 남긴다.
 * <p>
 * 친밀도 점수 갱신처럼 아웃박스를 두지 않기로 한 비동기 작업은 종료 드레인이 유일한 완충이다.
 * 드레인 시간을 넘겨 버려지는 작업이 있어도 어디에도 기록되지 않으면, 유실이 일어난 사실 자체를
 * 알 수 없다. 버린 개수가 로그에 남아야 드레인 시간을 늘릴지 판단할 수 있다.
 */
@Slf4j
public class ShutdownReportingTaskExecutor extends ThreadPoolTaskExecutor {

    @Override
    public void shutdown() {
        super.shutdown();

        int unprocessed = getQueueSize();
        if (unprocessed > 0) {
            log.error("Discarded {} async tasks still queued after the shutdown drain", unprocessed);
        }
    }
}
