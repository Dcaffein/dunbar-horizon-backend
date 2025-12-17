package com.example.GooRoomBe.social.common;

import com.example.GooRoomBe.global.userReference.SocialUserNotFoundException;
import com.example.GooRoomBe.global.userReference.SocialUser;
import com.example.GooRoomBe.global.userReference.SocialUserRepository;
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