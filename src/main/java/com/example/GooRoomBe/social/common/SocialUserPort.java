package com.example.GooRoomBe.social.common;

import com.example.GooRoomBe.global.userReference.SocialUser;

public interface SocialUserPort {
    SocialUser getUser(String userId);
}