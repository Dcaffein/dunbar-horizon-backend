package com.example.DunbarHorizon.flag.application.service.flag;

import com.example.DunbarHorizon.flag.application.port.in.FlagHostUseCase;
import com.example.DunbarHorizon.flag.application.port.in.FlagMemorialCommandUseCase;
import com.example.DunbarHorizon.flag.application.port.in.command.FlagHostCommand;
import com.example.DunbarHorizon.flag.adapter.out.persistence.jpa.FlagJpaRepository;
import com.example.DunbarHorizon.flag.domain.flag.event.FlagConcludedEvent;
import com.example.DunbarHorizon.support.TestContainerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestContainerConfig.class, FlagConclusionEventIntegrationTest.RecordingConfig.class})
class FlagConclusionEventIntegrationTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class RecordingConfig {
        @Bean
        ConclusionRecorder conclusionRecorder() {
            return new ConclusionRecorder();
        }
    }

    static class ConclusionRecorder {
        private final List<FlagConcludedEvent> received = new CopyOnWriteArrayList<>();

        @EventListener
        void on(FlagConcludedEvent event) {
            received.add(event);
        }

        List<FlagConcludedEvent> received() {
            return received;
        }

        void clear() {
            received.clear();
        }
    }

    @Autowired private FlagHostUseCase flagHostUseCase;
    @Autowired private FlagMemorialCommandUseCase memorialCommandUseCase;
    @Autowired private FlagJpaRepository flagJpaRepository;
    @Autowired private ConclusionRecorder recorder;

    private static final Long HOST_ID = 4201L;

    private Long endedFlagId;

    @BeforeEach
    void setUp() {
        recorder.clear();
        LocalDateTime base = LocalDateTime.now().withNano(0);
        endedFlagId = flagHostUseCase.hostFlag(new FlagHostCommand(
                HOST_ID, "종료된 모임", "설명", 10,
                base.minusHours(4), base.minusHours(3), base.minusHours(1)));
    }

    @Test
    @DisplayName("후기를 쓰고 지워도 결말 이벤트가 나가지 않고 보존 필드가 바뀌지 않는다")
    void memorialWriteAndDelete_concludesNothing() {
        // when
        Long memorialId = memorialCommandUseCase.createMemorial(endedFlagId, HOST_ID, "후기");
        boolean preservedAfterWrite = flagJpaRepository.findById(endedFlagId).orElseThrow().isPreserved();
        memorialCommandUseCase.deleteMemorial(endedFlagId, memorialId, HOST_ID);

        // then
        assertThat(recorder.received()).isEmpty();
        assertThat(preservedAfterWrite).isFalse();
        assertThat(flagJpaRepository.findById(endedFlagId).orElseThrow().isPreserved()).isFalse();
    }
}
