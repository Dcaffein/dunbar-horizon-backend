package com.example.DunbarHorizon.account.application.service;

import com.example.DunbarHorizon.account.application.dto.AuthTokenResult;
import com.example.DunbarHorizon.account.application.port.out.PasswordHasher;
import com.example.DunbarHorizon.account.domain.Auth;
import com.example.DunbarHorizon.account.domain.AuthProvider;
import com.example.DunbarHorizon.account.domain.User;
import com.example.DunbarHorizon.account.domain.exception.AlreadyRegisteredEmailException;
import com.example.DunbarHorizon.account.domain.exception.InvalidVerificationTokenException;
import com.example.DunbarHorizon.account.domain.repository.AuthRepository;
import com.example.DunbarHorizon.account.domain.repository.PendingSignupRepository;
import com.example.DunbarHorizon.account.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SignupService {
    private final UserRepository userRepository;
    private final AuthRepository authRepository;
    private final PendingSignupRepository pendingSignupRepository;
    private final PasswordHasher passwordHasher;
    private final LoginService loginService;

    public AuthTokenResult signup(String token, String password, String nickname) {
        // 조회와 삭제가 한 연산(GETDEL)이라 동시 요청이 같은 토큰으로 두 번 계정을 만들 수 없다.
        String email = pendingSignupRepository.consumeEmailByToken(token)
                .orElseThrow(InvalidVerificationTokenException::new);

        // 접수 시점에는 미가입이었어도 링크를 여는 사이에 다른 경로(OAuth)로 가입될 수 있다.
        userRepository.findByEmail(email).ifPresent(u -> {
            throw new AlreadyRegisteredEmailException(email);
        });

        // Auth를 저장하기 전에 User ID를 발급받는다.
        User user = userRepository.save(User.createActive(email, nickname));
        authRepository.save(Auth.createLocalAuth(user.getId(), passwordHasher.hash(password)));


        return loginService.issueTokens(user);
    }

    public User registerOAuthUser(String email, String nickname, AuthProvider provider, String providerId) {
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(User.createActive(email, nickname)));

        if (!authRepository.existsByUserIdAndProviderAndProviderId(user.getId(), provider, providerId)) {
            authRepository.save(Auth.createOAuth(user.getId(), provider, providerId));
        }

        return user;
    }

}
