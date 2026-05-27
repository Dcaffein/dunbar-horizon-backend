package com.example.DunbarHorizon.account.application.service;

import com.example.DunbarHorizon.account.application.port.in.VerificationUseCase;
import com.example.DunbarHorizon.account.application.port.out.EmailPort;
import com.example.DunbarHorizon.account.domain.exception.*;
import com.example.DunbarHorizon.account.domain.Auth;
import com.example.DunbarHorizon.account.domain.AuthProvider;
import com.example.DunbarHorizon.account.domain.User;
import com.example.DunbarHorizon.account.domain.repository.AuthRepository;
import com.example.DunbarHorizon.account.domain.repository.EmailVerificationTokenRepository;
import com.example.DunbarHorizon.account.domain.repository.UserRepository;
import com.example.DunbarHorizon.global.util.UuidUtil;
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
    public void sendVerificationEmail(String email, String redirectPage) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("가입되지 않은 이메일입니다."));

        Auth localAuth = authRepository.findByUserIdAndProvider(user.getId(), AuthProvider.LOCAL)
                .orElseThrow(() -> new AuthNotFoundException("로컬 가입 정보가 존재하지 않습니다."));

        if (localAuth.isVerified()) {
            throw new AlreadyRegisteredEmailException(email);
        }

        verificationTokenRepository.deleteByUserId(user.getId());

        String token = UuidUtil.createV7().toString();
        verificationTokenRepository.save(user.getId(), token);

        emailPort.sendVerificationEmail(user.getEmail(), token, redirectPage);
    }

    @Override
    public void verifyEmail(String tokenStr) {
        Long userId = verificationTokenRepository.findUserIdByToken(tokenStr)
                .orElseThrow(() -> new VerificationTokenNotFoundException("유효하지 않거나 존재하지 않는 인증 토큰입니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        Auth localAuth = authRepository.findByUserIdAndProvider(userId, AuthProvider.LOCAL)
                .orElseThrow(() -> new AuthNotFoundException("존재하지 않는 인증 정보입니다."));

        localAuth.verify();
        user.activate();
        authRepository.save(localAuth);
        userRepository.save(user);
        verificationTokenRepository.deleteByUserId(userId);
    }
}
