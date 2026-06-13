package com.example.DunbarHorizon.flag.adapter.in.web;

import com.example.DunbarHorizon.flag.application.service.flag.FlagSeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@Profile("local")
@RestController
@RequestMapping("/api/dev/flags")
@RequiredArgsConstructor
public class FlagSeedController {

    private final FlagSeedService flagSeedService;

    @PostMapping("/seed")
    public ResponseEntity<FlagSeedResponse> seed(@RequestBody FlagSeedRequest request) {
        List<Long> ids = flagSeedService.seed(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new FlagSeedResponse(ids));
    }

    public record FlagSeedRequest(Long hostUserId, List<FlagSeedItem> flags) {
        public record FlagSeedItem(
                String title,
                String status,
                ScheduleSeedItem schedule,
                Integer capacity,
                List<Long> participantUserIds,
                List<MemorialSeedItem> memorials
        ) {}

        public record ScheduleSeedItem(
                LocalDateTime startDateTime,
                LocalDateTime endDateTime,
                LocalDateTime deadline
        ) {}
    }

    public record MemorialSeedItem(Long writerUserId, String content) {}

    public record FlagSeedResponse(List<Long> flagIds) {}
}
