package com.example.DunbarHorizon.buzz.application.dto.info;

import com.example.DunbarHorizon.buzz.domain.Buzz;

public record ReceivedBuzz(
        Buzz buzz,
        boolean read
) {}
