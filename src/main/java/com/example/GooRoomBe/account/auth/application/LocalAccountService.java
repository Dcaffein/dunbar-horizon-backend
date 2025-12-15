package com.example.GooRoomBe.account.auth.application;

import com.example.GooRoomBe.account.auth.domain.LocalAuth;
import com.example.GooRoomBe.account.auth.domain.token.VerificationToken;
import com.example.GooRoomBe.account.auth.exception.InvalidVerificationTokenException;
import com.example.GooRoomBe.account.auth.repository.LocalAuthRepository;
import com.example.GooRoomBe.account.auth.repository.VerificationTokenRepository;
import com.example.GooRoomBe.account.user.api.dto.UserSignupRequestDto;
import com.example.GooRoomBe.account.user.domain.User;
import com.example.GooRoomBe.account.user.domain.UserFactory;
import com.example.GooRoomBe.account.user.exception.UserNotFoundException;
import com.example.GooRoomBe.account.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LocalAccountService {

    private final UserRepository userRepository;
    private final LocalAuthRepository localAuthRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final UserFactory userFactory;

    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void signUp(UserSignupRequestDto dto) {
        User newUser = userFactory.createUnverifiedUser(dto.nickname(), dto.email());
        LocalAuth newLocalAuth = new LocalAuth(newUser, passwordEncoder.encode(dto.password()));
        localAuthRepository.save(newLocalAuth);
    }

    @Transactional
    public void sendVerificationEmail(String email, String redirectPage) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("해당 이메일을 가진 user가 없습니다. " + email));
        user.isActive();

        verificationTokenRepository.findByUser_Id(user.getId())
                .ifPresent(verificationTokenRepository::delete);

        VerificationToken newVerificationToken = new VerificationToken(user);
        verificationTokenRepository.save(newVerificationToken);

        emailService.sendVerificationEmail(user.getEmail(), newVerificationToken.getTokenValue(), redirectPage);
    }

    @Transactional
    public void verifyEmail(String token) {
        VerificationToken verificationToken = verificationTokenRepository.findByTokenValue(token)
                .orElseThrow(InvalidVerificationTokenException::new);

        verificationToken.validateExpiration();

        User user = verificationToken.getUser();
        user.verify();
        userRepository.save(user);

        verificationTokenRepository.delete(verificationToken);
    }
}
