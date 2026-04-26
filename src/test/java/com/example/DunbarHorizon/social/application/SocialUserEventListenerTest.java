package com.example.DunbarHorizon.social.application;

import com.example.DunbarHorizon.account.domain.outbox.UserOutboxEventType;
import com.example.DunbarHorizon.global.event.user.UserSyncCompletedEvent;
import com.example.DunbarHorizon.global.event.user.UserSyncIntegrationEvent;
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
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SocialUserEventListenerTest {

    @InjectMocks
    private SocialUserEventListener socialUserEventListener;

    @Mock
    private SocialUserRepository socialUserRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("ACTIVATE 이벤트 수신 시 SocialUser가 없으면 새로 생성하고 완료 이벤트를 발행한다")
    void onUserSync_Activate_NewUser_CreatesSocialUserAndPublishesCompleted() {
        // given
        UserSyncIntegrationEvent event = new UserSyncIntegrationEvent(
                "outbox-1", 1L, UserOutboxEventType.ACTIVATE, "테스트유저", "https://example.com/image.png", null
        );
        given(socialUserRepository.findById(1L)).willReturn(Optional.empty());
        given(socialUserRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // when
        socialUserEventListener.onUserSync(event);

        // then
        ArgumentCaptor<SocialUser> captor = ArgumentCaptor.forClass(SocialUser.class);
        verify(socialUserRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(1L);
        assertThat(captor.getValue().getNickname()).isEqualTo("테스트유저");

        ArgumentCaptor<UserSyncCompletedEvent> completedCaptor = ArgumentCaptor.forClass(UserSyncCompletedEvent.class);
        verify(eventPublisher).publishEvent(completedCaptor.capture());
        assertThat(completedCaptor.getValue().outboxId()).isEqualTo("outbox-1");
    }

    @Test
    @DisplayName("ACTIVATE 이벤트 수신 시 SocialUser가 이미 존재하면 활성화 상태로 전환한다")
    void onUserSync_Activate_ExistingUser_ReactivatesSocialUser() {
        // given
        UserSyncIntegrationEvent event = new UserSyncIntegrationEvent(
                "outbox-2", 1L, UserOutboxEventType.ACTIVATE, "테스트유저", null, null
        );
        SocialUser existing = new SocialUser(1L, "테스트유저", null);
        existing.switchUserStatus(false);
        given(socialUserRepository.findById(1L)).willReturn(Optional.of(existing));
        given(socialUserRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // when
        socialUserEventListener.onUserSync(event);

        // then
        verify(socialUserRepository).save(existing);
        verify(eventPublisher).publishEvent(any(UserSyncCompletedEvent.class));
    }

    @Test
    @DisplayName("DEACTIVATE 이벤트 수신 시 SocialUser를 비활성화하고 완료 이벤트를 발행한다")
    void onUserSync_Deactivate_DeactivatesSocialUser() {
        // given
        UserSyncIntegrationEvent event = new UserSyncIntegrationEvent(
                "outbox-3", 1L, UserOutboxEventType.DEACTIVATE, null, null, null
        );
        SocialUser socialUser = new SocialUser(1L, "테스트유저", null);
        given(socialUserRepository.findById(1L)).willReturn(Optional.of(socialUser));
        given(socialUserRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // when
        socialUserEventListener.onUserSync(event);

        // then
        verify(socialUserRepository).save(socialUser);
        verify(eventPublisher).publishEvent(any(UserSyncCompletedEvent.class));
    }

    @Test
    @DisplayName("DEACTIVATE 이벤트 수신 시 SocialUser가 없으면 아무것도 하지 않는다")
    void onUserSync_Deactivate_NoSocialUser_DoesNothing() {
        // given
        UserSyncIntegrationEvent event = new UserSyncIntegrationEvent(
                "outbox-4", 99L, UserOutboxEventType.DEACTIVATE, null, null, null
        );
        given(socialUserRepository.findById(99L)).willReturn(Optional.empty());

        // when
        socialUserEventListener.onUserSync(event);

        // then
        verify(socialUserRepository, never()).save(any());
        verify(eventPublisher).publishEvent(any(UserSyncCompletedEvent.class));
    }

    @Test
    @DisplayName("Neo4j 처리 중 예외가 발생하면 완료 이벤트를 발행하지 않는다")
    void onUserSync_Exception_DoesNotPublishCompleted() {
        // given
        UserSyncIntegrationEvent event = new UserSyncIntegrationEvent(
                "outbox-5", 4L, UserOutboxEventType.ACTIVATE, "nick", null, null
        );
        given(socialUserRepository.findById(4L)).willThrow(new RuntimeException("Neo4j error"));

        // when
        socialUserEventListener.onUserSync(event);

        // then
        verify(eventPublisher, never()).publishEvent(any(UserSyncCompletedEvent.class));
    }

    @Test
    @DisplayName("PROFILE_UPDATE 이벤트가 최신이면 SocialUser 프로필을 갱신하고 완료 이벤트를 발행한다")
    void onUserSync_ProfileUpdate_NewerEvent_UpdatesProfileAndPublishesCompleted() {
        // given
        LocalDateTime oldTime = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime newTime = LocalDateTime.of(2024, 6, 1, 0, 0);

        SocialUser socialUser = new SocialUser(10L, "옛날닉네임", null);
        socialUser.updateProfile("옛날닉네임", null, oldTime);

        UserSyncIntegrationEvent event = new UserSyncIntegrationEvent(
                "outbox-6", 10L, UserOutboxEventType.PROFILE_UPDATE, "새닉네임", "https://new.img", newTime
        );
        given(socialUserRepository.findById(10L)).willReturn(Optional.of(socialUser));
        given(socialUserRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // when
        socialUserEventListener.onUserSync(event);

        // then
        assertThat(socialUser.getNickname()).isEqualTo("새닉네임");
        assertThat(socialUser.getProfileImageUrl()).isEqualTo("https://new.img");
        verify(socialUserRepository).save(socialUser);
        verify(eventPublisher).publishEvent(any(UserSyncCompletedEvent.class));
    }

    @Test
    @DisplayName("PROFILE_UPDATE 이벤트가 오래된 것이면 LWW 규칙에 따라 프로필을 갱신하지 않는다")
    void onUserSync_ProfileUpdate_OlderEvent_SkipsUpdateDueToLww() {
        // given
        LocalDateTime recentTime = LocalDateTime.of(2024, 6, 1, 0, 0);
        LocalDateTime staleTime = LocalDateTime.of(2024, 1, 1, 0, 0);

        SocialUser socialUser = new SocialUser(10L, "현재닉네임", "https://current.img");
        socialUser.updateProfile("현재닉네임", "https://current.img", recentTime);

        UserSyncIntegrationEvent event = new UserSyncIntegrationEvent(
                "outbox-7", 10L, UserOutboxEventType.PROFILE_UPDATE, "오래된닉네임", null, staleTime
        );
        given(socialUserRepository.findById(10L)).willReturn(Optional.of(socialUser));
        given(socialUserRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // when
        socialUserEventListener.onUserSync(event);

        // then
        assertThat(socialUser.getNickname()).isEqualTo("현재닉네임");
        assertThat(socialUser.getProfileImageUrl()).isEqualTo("https://current.img");
        verify(eventPublisher).publishEvent(any(UserSyncCompletedEvent.class));
    }

    @Test
    @DisplayName("PROFILE_UPDATE 이벤트 수신 시 SocialUser가 없으면 저장하지 않고 완료 이벤트를 발행한다")
    void onUserSync_ProfileUpdate_NoSocialUser_DoesNotSave() {
        // given
        UserSyncIntegrationEvent event = new UserSyncIntegrationEvent(
                "outbox-8", 99L, UserOutboxEventType.PROFILE_UPDATE, "닉네임", null,
                LocalDateTime.now()
        );
        given(socialUserRepository.findById(99L)).willReturn(Optional.empty());

        // when
        socialUserEventListener.onUserSync(event);

        // then
        verify(socialUserRepository, never()).save(any());
        verify(eventPublisher).publishEvent(any(UserSyncCompletedEvent.class));
    }
}
