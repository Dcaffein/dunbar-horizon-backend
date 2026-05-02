package com.example.DunbarHorizon.flag.adapter.in.web;

import com.example.DunbarHorizon.flag.application.dto.result.FlagResult;
import com.example.DunbarHorizon.flag.application.port.in.FlagQueryUseCase;
import com.example.DunbarHorizon.global.annotation.CurrentUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/flags")
@RequiredArgsConstructor
public class FlagQueryController {

    private final FlagQueryUseCase flagQueryUseCase;

    @GetMapping("/me/hosting")
    public ResponseEntity<List<FlagResult>> getMyHostingFlags(
            @CurrentUserId Long currentUserId
    ) {
        return ResponseEntity.ok(flagQueryUseCase.getMyHostingFlags(currentUserId));
    }

    @GetMapping("/me/participating")
    public ResponseEntity<List<FlagResult>> getMyParticipatingFlags(
            @CurrentUserId Long currentUserId
    ) {
        return ResponseEntity.ok(flagQueryUseCase.getParticipatingFlags(currentUserId));
    }
}
