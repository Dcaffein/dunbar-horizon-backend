package com.example.DunbarHorizon.social.application;

import com.example.DunbarHorizon.global.event.user.UserActivatedEvent;
import com.example.DunbarHorizon.global.event.user.UserDeactivatedEvent;
import com.example.DunbarHorizon.social.application.eventHandler.SocialUserEventListener;
import com.example.DunbarHorizon.social.domain.socialUser.SocialUser;
import com.example.DunbarHorizon.social.domain.socialUser.repository.SocialUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SocialUserEventListenerTest {

    @InjectMocks
    private SocialUserEventListener socialUserEventListener;

    @Mock
    private SocialUserRepository socialUserRepository;

    @Test
    @DisplayName("UserActivatedEvent 수신 시 SocialUser가 없으면 새로 생성한다")
    void onUserActivated_NewUser_CreatesSocialUser() {
        // given
        UserActivatedEvent event = new UserActivatedEvent(1L, "테스트유저", "https://example.com/image.png");
        given(socialUserRepository.findById(1L)).willReturn(Optional.empty());

        // when
        socialUserEventListener.onUserActivated(event);

        // then
        ArgumentCaptor<SocialUser> captor = ArgumentCaptor.forClass(SocialUser.class);
        verify(socialUserRepository).save(captor.capture());

        SocialUser saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(event.userId());
        assertThat(saved.getNickname()).isEqualTo(event.nickname());
        assertThat(saved.getProfileImageUrl()).isEqualTo(event.profileImageUrl());
    }

    @Test
    @DisplayName("UserActivatedEvent 수신 시 SocialUser가 이미 존재하면 활성화 상태로 전환한다")
    void onUserActivated_ExistingUser_ReactivatesSocialUser() {
        // given
        UserActivatedEvent event = new UserActivatedEvent(1L, "테스트유저", null);
        SocialUser existingSocialUser = new SocialUser(1L, "테스트유저", null);
        existingSocialUser.switchUserStatus(false);
        given(socialUserRepository.findById(1L)).willReturn(Optional.of(existingSocialUser));

        // when
        socialUserEventListener.onUserActivated(event);

        // then
        verify(socialUserRepository).save(existingSocialUser);
    }

    @Test
    @DisplayName("UserDeactivatedEvent 수신 시 SocialUser를 비활성화 상태로 전환한다")
    void onUserDeactivated_DeactivatesSocialUser() {
        // given
        UserDeactivatedEvent event = new UserDeactivatedEvent(1L);
        SocialUser socialUser = new SocialUser(1L, "테스트유저", null);
        given(socialUserRepository.findById(1L)).willReturn(Optional.of(socialUser));

        // when
        socialUserEventListener.onUserDeactivated(event);

        // then
        verify(socialUserRepository).save(socialUser);
    }

    @Test
    @DisplayName("UserDeactivatedEvent 수신 시 SocialUser가 없으면 아무것도 하지 않는다")
    void onUserDeactivated_NoSocialUser_DoesNothing() {
        // given
        UserDeactivatedEvent event = new UserDeactivatedEvent(99L);
        given(socialUserRepository.findById(99L)).willReturn(Optional.empty());

        // when
        socialUserEventListener.onUserDeactivated(event);

        // then
        verify(socialUserRepository, never()).save(any());
    }
}
