package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.application.dto.result.SocialProfileResult;
import com.example.DunbarHorizon.social.application.port.out.ImageUrlResolverPort;
import com.example.DunbarHorizon.social.domain.socialUser.SocialUser;
import com.example.DunbarHorizon.social.domain.socialUser.exception.UserReferenceNotFoundException;
import com.example.DunbarHorizon.social.domain.socialUser.repository.SocialUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SocialUserServiceTest {

    @InjectMocks private SocialUserService socialUserService;
    @Mock private SocialUserRepository socialUserRepository;
    @Mock private SocialUserSyncHelper syncHelper;
    @Mock private ImageUrlResolverPort imageUrlResolverPort;

    @Test
    @DisplayName("Neo4j에 존재하는 유저 ID로 조회하면 SocialProfileResult를 반환한다")
    void getSocialProfile_UserExists_ReturnsSocialProfile() {
        // given
        String key = "profiles/uuid-photo";
        String resolvedUrl = "https://presigned.s3.url/friend.png";
        SocialUser socialUser = new SocialUser(2L, "친구", key);
        given(socialUserRepository.findAllUserReferencesById(Set.of(2L))).willReturn(Set.of(socialUser));
        given(imageUrlResolverPort.resolveUrl(key)).willReturn(resolvedUrl);

        // when
        SocialProfileResult result = socialUserService.getSocialProfile(2L);

        // then
        assertThat(result.id()).isEqualTo(2L);
        assertThat(result.nickname()).isEqualTo("친구");
        assertThat(result.profileImageUrl()).isEqualTo(resolvedUrl);
    }

    @Test
    @DisplayName("Neo4j에 없고 sync도 실패하면 UserReferenceNotFoundException을 던진다")
    void getSocialProfile_UserNotFound_ThrowsException() {
        // given
        given(socialUserRepository.findAllUserReferencesById(Set.of(999L))).willReturn(Set.of());
        given(syncHelper.syncAndSave(Set.of(999L))).willReturn(Set.of());

        // when & then
        assertThatThrownBy(() -> socialUserService.getSocialProfile(999L))
                .isInstanceOf(UserReferenceNotFoundException.class);
    }
}
