package com.example.GooRoomBe.account.application.port.in;

import com.example.GooRoomBe.account.application.port.in.dto.UserProfileDto;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserQueryUseCase {
    List<UserProfileDto> getUserProfiles(Collection<Long> ids);
    Optional<UserProfileDto> getActiveUserProfile(Long id);
}
