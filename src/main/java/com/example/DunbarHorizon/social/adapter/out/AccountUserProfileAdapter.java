package com.example.DunbarHorizon.social.adapter.out;

import com.example.DunbarHorizon.account.application.dto.UserProfileInfo;
import com.example.DunbarHorizon.account.application.port.in.UserQueryUseCase;
import com.example.DunbarHorizon.social.application.port.out.UserProfilePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AccountUserProfileAdapter implements UserProfilePort {

    private final UserQueryUseCase userQueryUseCase;

    @Override
    public Optional<UserProfileInfo> getActiveUserProfile(Long id) {
        return userQueryUseCase.getActiveUserProfile(id);
    }

    @Override
    public List<UserProfileInfo> getUserProfiles(Collection<Long> ids) {
        return userQueryUseCase.getUserProfiles(ids);
    }
}
