package com.example.DunbarHorizon.account.adapter.out.persistence;

import com.example.DunbarHorizon.account.adapter.out.persistence.jpa.EmailVerificationTokenJpaRepository;
import com.example.DunbarHorizon.account.domain.model.EmailVerificationToken;
import com.example.DunbarHorizon.account.domain.model.User;
import com.example.DunbarHorizon.account.domain.repository.EmailVerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class EmailVerificationTokenRepositoryAdapter implements EmailVerificationTokenRepository {

    private final EmailVerificationTokenJpaRepository emailVerificationTokenJpaRepository;

    @Override
    public Optional<EmailVerificationToken> findByToken(String token) {
        return emailVerificationTokenJpaRepository.findByToken(token);
    }

    @Override
    public void deleteByUser(User user) {
        emailVerificationTokenJpaRepository.deleteByUser(user);
    }

    @Override
    public void delete(EmailVerificationToken token) {
        emailVerificationTokenJpaRepository.delete(token);
    }

    @Override
    public EmailVerificationToken save(EmailVerificationToken token) {
        return emailVerificationTokenJpaRepository.save(token);
    }

    @Override
    public void flush() {
        emailVerificationTokenJpaRepository.flush();
    }
}
