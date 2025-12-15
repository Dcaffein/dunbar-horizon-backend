package com.example.GooRoomBe.social.socialUser.infrastructure;

import com.example.GooRoomBe.social.socialUser.SocialUserNotFoundException;
import com.example.GooRoomBe.social.socialUser.SocialUser;
import com.example.GooRoomBe.social.socialUser.SocialUserPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SocialUserAdapter implements SocialUserPort {

    private final SocialUserRepository socialUserRepository;

    @Override
    public SocialUser getUser(String userId) {
        return socialUserRepository.findById(userId)
                .orElseThrow(() -> new SocialUserNotFoundException(userId));
    }
}