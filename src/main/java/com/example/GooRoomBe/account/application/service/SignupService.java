package com.example.GooRoomBe.account.application.service;

import com.example.GooRoomBe.account.application.port.in.SignupUseCase;
import com.example.GooRoomBe.account.application.port.out.PasswordHasher;
import com.example.GooRoomBe.global.event.user.UserActivatedEvent;
import com.example.GooRoomBe.account.domain.exception.AlreadyRegisteredEmailException;
import com.example.GooRoomBe.account.domain.model.Auth;
import com.example.GooRoomBe.account.domain.model.AuthProvider;
import com.example.GooRoomBe.account.domain.model.User;
import com.example.GooRoomBe.account.domain.repository.AuthRepository;
import com.example.GooRoomBe.account.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SignupService implements SignupUseCase {
    private final UserRepository userRepository;
    private final AuthRepository authRepository;
    private final PasswordHasher passwordHasher;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void signup(String email, String password, String nickname) {
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(User.builder().email(email).nickname(nickname).build()));

        if (!user.isPending()) throw new AlreadyRegisteredEmailException(email);

        user.overwritePendingProfile(nickname);

        String encodedPassword = passwordHasher.encode(password);

        Auth auth = authRepository.findByUserIdAndProvider(user.getId(), AuthProvider.LOCAL)
                .map(existingAuth -> {
                    existingAuth.overwritePassword(encodedPassword, email);
                    return existingAuth;
                })
                .orElseGet(() -> Auth.createLocalAuth(user.getId(), encodedPassword));

        authRepository.save(auth);
    }

    @Override
    public User registerOAuthUser(String email, String nickname, AuthProvider provider, String providerId) {

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = User.createActiveOAuthUser(email, nickname);
                    userRepository.save(newUser);
                    eventPublisher.publishEvent(new UserActivatedEvent(newUser.getId(), newUser.getNickname(), newUser.getProfileImage()));

                    return newUser;
                });

        if (user.isPending()) {
            user.overwritePendingProfile(nickname);
            user.activate();
            userRepository.save(user);
        }

        if (!authRepository.existsByUserIdAndProviderAndProviderId(user.getId(), provider, providerId)) {
            authRepository.save(Auth.createOAuth(user.getId(), provider, providerId));
        }

        return user;
    }
}
