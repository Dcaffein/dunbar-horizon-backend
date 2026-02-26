package com.example.GooRoomBe.account.application.service;

import com.example.GooRoomBe.account.application.port.in.VerificationUseCase;
import com.example.GooRoomBe.account.application.port.out.EmailPort;
import com.example.GooRoomBe.account.domain.exception.*;
import com.example.GooRoomBe.account.domain.model.Auth;
import com.example.GooRoomBe.account.domain.model.AuthProvider;
import com.example.GooRoomBe.account.domain.model.EmailVerificationToken;
import com.example.GooRoomBe.account.domain.model.User;
import com.example.GooRoomBe.account.domain.repository.AuthRepository;
import com.example.GooRoomBe.account.domain.repository.EmailVerificationTokenRepository;
import com.example.GooRoomBe.account.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class VerificationService implements VerificationUseCase {
    private final UserRepository userRepository;
    private final AuthRepository authRepository;
    private final EmailVerificationTokenRepository verificationTokenRepository;
    private final EmailPort emailPort;

    @Override
    public void sendVerificationEmail(String email,String redirectPage) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("가입되지 않은 이메일입니다."));

        Auth localAuth = authRepository.findByUserIdAndProvider(user.getId(), AuthProvider.LOCAL)
                .orElseThrow(() -> new AuthNotFoundException("로컬 가입 정보가 존재하지 않습니다."));

        if (localAuth.isVerified()) {
            throw new AlreadyRegisteredEmailException(email);
        }

        verificationTokenRepository.deleteByUser(user);
        verificationTokenRepository.flush();

        EmailVerificationToken newToken = new EmailVerificationToken(user);
        verificationTokenRepository.save(newToken);

        emailPort.sendVerificationEmail(
                user.getEmail(),
                newToken.getToken(),
                redirectPage
        );
    }

    @Override
    public void verifyEmail(String tokenStr) {
        EmailVerificationToken token = verificationTokenRepository.findByToken(tokenStr)
                .orElseThrow(() -> new VerificationTokenNotFoundException("유효하지 않거나 존재하지 않는 인증 토큰입니다."));

        if (token.isExpired()) {
            throw new ExpiredTokenException();
        }

        User user = token.getUser();
        Auth localAuth = authRepository.findByUserIdAndProvider(user.getId(), AuthProvider.LOCAL)
                .orElseThrow(() -> new AuthNotFoundException("존재하지 않는 인증 정보입니다."));

        localAuth.verify();
        user.activate();
        authRepository.save(localAuth);
        userRepository.save(user);
        verificationTokenRepository.delete(token);
    }
}