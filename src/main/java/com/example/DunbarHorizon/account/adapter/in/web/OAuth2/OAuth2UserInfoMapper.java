package com.example.DunbarHorizon.account.adapter.in.web.OAuth2;

import com.example.DunbarHorizon.account.adapter.in.web.OAuth2.OAuth2UserInfo.GoogleOAuth2UserInfo;
import com.example.DunbarHorizon.account.adapter.in.web.OAuth2.OAuth2UserInfo.OAuth2UserInfo;
import com.example.DunbarHorizon.account.domain.model.AuthProvider;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OAuth2UserInfoMapper {

    public static OAuth2UserInfo parse(AuthProvider provider, Map<String, Object> attributes) {
        return switch (provider) {
            case GOOGLE -> new GoogleOAuth2UserInfo(attributes);
            default     -> throw new IllegalArgumentException("지원하지 않는 소셜 로그인입니다: " + provider);
        };
    }
}