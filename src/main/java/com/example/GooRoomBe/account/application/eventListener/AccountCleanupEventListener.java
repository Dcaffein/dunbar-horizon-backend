package com.example.GooRoomBe.account.application.eventListener;

import com.example.GooRoomBe.global.event.user.UserActivatedEvent;
import com.example.GooRoomBe.account.domain.repository.AuthRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AccountCleanupEventListener {

    private final AuthRepository authRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cleanUpGarbageAuths(UserActivatedEvent event) {
        authRepository.deleteUnverifiedByUserId(event.userId());
    }
}