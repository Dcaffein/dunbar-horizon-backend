package com.example.DunbarHorizon.buzz.application.service;

import com.example.DunbarHorizon.buzz.application.dto.view.BuzzUserProfileView;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzProfileResult;
import com.example.DunbarHorizon.buzz.application.port.out.ImageStoragePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BuzzProfileAssembler {

    private final ImageStoragePort imageStoragePort;

    public BuzzProfileResult profileOf(Long userId, BuzzUserProfileView profile) {
        if (profile == null) {
            return new BuzzProfileResult(userId, null, null);
        }
        return new BuzzProfileResult(userId, profile.nickname(), imageStoragePort.resolveUrl(profile.profileImageKey()));
    }
}
