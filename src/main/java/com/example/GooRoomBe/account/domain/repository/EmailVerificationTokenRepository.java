package com.example.GooRoomBe.account.domain.repository;

import com.example.GooRoomBe.account.domain.model.EmailVerificationToken;
import com.example.GooRoomBe.account.domain.model.User;

import java.util.Optional;

public interface EmailVerificationTokenRepository {
    Optional<EmailVerificationToken> findByToken(String token);
    void deleteByUser(User user);
    void delete(EmailVerificationToken token);
    EmailVerificationToken save(EmailVerificationToken token);
    void flush();
}