package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.account.application.dto.UserProfileInfo;
import com.example.DunbarHorizon.social.application.dto.result.SocialProfileResult;
import com.example.DunbarHorizon.social.application.port.out.UserProfilePort;
import com.example.DunbarHorizon.social.domain.socialUser.exception.UserReferenceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AccountSocialUserQueryServiceTest {

    @InjectMocks private AccountSocialUserQueryService service;
    @Mock private UserProfilePort userProfilePort;

    @Test
    @DisplayName("계정 원본의 ID, 닉네임, 이미지 URL을 소셜 프로필로 반환한다")
    void getSocialProfile_ReturnsAccountProfile_ActiveUser() {
        // given
        String imageUrl = "https://example.com/profile.png";
        given(userProfilePort.getActiveUserProfile(2L))
                .willReturn(Optional.of(new UserProfileInfo(2L, "친구", imageUrl)));

        // when
        SocialProfileResult result = service.getSocialProfile(2L);

        // then
        assertThat(result).isEqualTo(new SocialProfileResult(2L, "친구", imageUrl));
    }

    @Test
    @DisplayName("프로필 이미지가 없으면 소셜 응답에도 이미지 URL이 없다")
    void getSocialProfile_ReturnsNullImageUrl_NoProfileImage() {
        // given
        given(userProfilePort.getActiveUserProfile(2L))
                .willReturn(Optional.of(new UserProfileInfo(2L, "친구", null)));

        // when
        SocialProfileResult result = service.getSocialProfile(2L);

        // then
        assertThat(result.profileImageUrl()).isNull();
    }

    @Test
    @DisplayName("활성 계정을 찾을 수 없으면 기존 소셜 사용자 조회 예외를 던진다")
    void getSocialProfile_ThrowsUserReferenceNotFound_NoActiveUser() {
        // given
        given(userProfilePort.getActiveUserProfile(2L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.getSocialProfile(2L))
                .isInstanceOf(UserReferenceNotFoundException.class);
    }
}
