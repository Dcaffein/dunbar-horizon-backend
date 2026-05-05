package com.example.DunbarHorizon.account.application.eventListener;

import com.example.DunbarHorizon.account.domain.repository.AuthRepository;
import com.example.DunbarHorizon.global.event.user.UserActivatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AccountCleanupEventListenerTest {

    private AuthRepository authRepository;
    private AccountCleanupEventListener listener;

    @BeforeEach
    void setUp() {
        authRepository = mock(AuthRepository.class);
        listener = new AccountCleanupEventListener(authRepository);
    }

    @Test
    void 유저_활성화_이벤트_수신_시_미인증_Auth가_삭제된다() {
        // given
        UserActivatedEvent event = new UserActivatedEvent(1L, "testUser", null);

        // when
        listener.cleanUpGarbageAuths(event);

        // then
        verify(authRepository).deleteUnverifiedByUserId(1L);
    }
}
