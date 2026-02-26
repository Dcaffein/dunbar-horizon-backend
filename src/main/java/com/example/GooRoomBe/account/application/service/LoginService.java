package com.example.GooRoomBe.account.application.service;

import com.example.GooRoomBe.account.application.port.in.LoginUseCase;
import com.example.GooRoomBe.account.application.port.in.dto.AuthPrincipal;
import com.example.GooRoomBe.account.application.port.in.dto.AuthTokenResult;
import com.example.GooRoomBe.account.application.port.out.AuthTokenProvider;
import com.example.GooRoomBe.account.application.port.out.PasswordHasher;
import com.example.GooRoomBe.account.domain.exception.NotVerifiedException;
import com.example.GooRoomBe.account.domain.exception.TokenTheftDetectedException;
import com.example.GooRoomBe.account.domain.exception.UserNotFoundException;
import com.example.GooRoomBe.account.domain.model.Auth;
import com.example.GooRoomBe.account.domain.model.AuthProvider;
import com.example.GooRoomBe.account.domain.model.RefreshToken;
import com.example.GooRoomBe.account.domain.model.User;
import com.example.GooRoomBe.account.domain.repository.AuthRepository;
import com.example.GooRoomBe.account.domain.repository.RefreshTokenRepository;
import com.example.GooRoomBe.account.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class LoginService implements LoginUseCase {
    private final UserRepository userRepository;
    private final AuthRepository authRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthTokenProvider authTokenProvider;
    private final PasswordHasher passwordHasher;

    @Override
    @Transactional(readOnly = true)
    public AuthTokenResult login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        Auth localAuth = authRepository.findByUserIdAndProvider(user.getId(), AuthProvider.LOCAL)
                .orElseThrow(() -> new BadCredentialsException("이메일/비밀번호를 확인해주세요."));

        if (!passwordHasher.matches(password, localAuth.getPassword())) {
            throw new BadCredentialsException("비밀번호가 일치하지 않습니다.");
        }

        if (!localAuth.isVerified()) {
            throw new NotVerifiedException(localAuth.getId());
        }

        return issueTokens(user);
    }

    @Override
    public AuthTokenResult issueTokens(User user) {
        AuthPrincipal principal = AuthPrincipal.from(user);
        String accessToken = authTokenProvider.createAccessToken(principal);
        String refreshTokenValue = authTokenProvider.createRefreshToken(principal);

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(user.getId())
                .tokenValue(refreshTokenValue)
                .build();
        refreshTokenRepository.save(refreshToken);

        return new AuthTokenResult(accessToken, refreshTokenValue);
    }

    @Override
    public AuthTokenResult reissue(String oldRefreshTokenValue) {
        AuthPrincipal authPrincipal = authTokenProvider.validateToken(oldRefreshTokenValue);

        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findValidToken(oldRefreshTokenValue, LocalDateTime.now());

        if (tokenOpt.isEmpty()) {
            refreshTokenRepository.deleteAllByUserId(authPrincipal.id());
            throw new TokenTheftDetectedException();
        }

        RefreshToken refreshToken = tokenOpt.get();
        String newAccessToken = authTokenProvider.createAccessToken(authPrincipal);
        String newRefreshToken = authTokenProvider.createRefreshToken(authPrincipal);

        refreshToken.rotateTokenValue(newRefreshToken);

        return new AuthTokenResult(newAccessToken, newRefreshToken);
    }

    @Override
    public void logout(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            return;
        }
        refreshTokenRepository.deleteByTokenValue(refreshTokenValue);
    }

}
