package com.example.GooRoomBe.account.adapter.out.email;

import com.example.GooRoomBe.account.application.port.out.EmailPort;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailAdapter implements EmailPort {

    private final JavaMailSender javaMailSender;

    @Value("${app.frontend.base-url}")
    private String defaultFrontendUrl;

    @Override
    @Async
    public void sendVerificationEmail(String email, String token, String redirectPage) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            String link = buildVerificationLink(token, redirectPage);

            String htmlContent = getVerificationHtml(link);

            helper.setTo(email);
            helper.setSubject("GooRoom 회원가입 이메일 인증");
            helper.setText(htmlContent, true);

            javaMailSender.send(mimeMessage);
            log.info("인증 이메일 전송 완료: {}", email);

        } catch (MailException | MessagingException e) {
            log.error("인증 이메일 전송 실패 (메일 서버 오류): {}", email, e);
        } catch (Exception e) {
            log.error("인증 이메일 전송 중 예상치 못한 오류 발생: {}", email, e);
        }
    }

    private String buildVerificationLink(String token, String redirectPage) {
        String path = (redirectPage == null || redirectPage.isBlank()) ? "/verify-email" : redirectPage.trim();
        if (!path.startsWith("/")) path = "/" + path;
        return defaultFrontendUrl + path + "?token=" + token;
    }

    private String getVerificationHtml(String link) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="margin: 0; padding: 0; background-color: #f4f4f4; font-family: sans-serif;">
                <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%%" style="padding: 20px 0;">
                    <tr>
                        <td align="center">
                            <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="600" style="background-color: #ffffff; border-radius: 8px; box-shadow: 0 2px 5px rgba(0,0,0,0.1);">
                                <tr><td align="center" style="padding: 30px; background-color: #007bff;"><h1 style="color: #ffffff; margin: 0;">GooRoom</h1></td></tr>
                                <tr><td style="padding: 40px;"><p>GooRoom 서비스 이용을 위해 아래 버튼을 클릭하여 인증을 완료해주세요.</p>
                                <div style="text-align: center; margin: 30px;"><a href="%s" style="padding: 14px 30px; background-color: #007bff; color: #ffffff; text-decoration: none; border-radius: 5px;">이메일 인증하기</a></div>
                                </td></tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(link);
    }
}