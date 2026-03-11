package com.example.DunbarHorizon.account.adapter.in.web.OAuth2;

import com.example.DunbarHorizon.account.adapter.in.web.OAuth2.OAuth2UserInfo.OAuth2UserInfo;
import com.example.DunbarHorizon.account.application.port.in.SignupUseCase;
import com.example.DunbarHorizon.account.domain.model.AuthProvider;
import com.example.DunbarHorizon.account.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final SignupUseCase signupUseCase;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        AuthProvider provider = AuthProvider.valueOf(userRequest.getClientRegistration().getRegistrationId().toUpperCase());

        OAuth2UserInfo userInfo = OAuth2UserInfoMapper.parse(provider, oAuth2User.getAttributes());

        User savedUser = signupUseCase.registerOAuthUser(
                userInfo.getEmail(),
                userInfo.getName(),
                provider,
                userInfo.getId()
        );

        return new CustomOAuth2User(savedUser, oAuth2User.getAttributes());
    }
}