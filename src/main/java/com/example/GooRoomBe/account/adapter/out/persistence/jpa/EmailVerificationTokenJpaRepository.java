package com.example.GooRoomBe.account.adapter.out.persistence.jpa;

import com.example.GooRoomBe.account.domain.model.EmailVerificationToken;
import com.example.GooRoomBe.account.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationTokenJpaRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByToken(String token);
    void deleteByUser(User user);
}
