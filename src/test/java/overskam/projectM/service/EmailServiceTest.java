package overskam.projectM.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EmailServiceTest {
    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final EmailService emailService = new EmailService(mailSender);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "from", "ProjectM <no-reply@test.com>");
        ReflectionTestUtils.setField(emailService, "verifyBaseUrl", "http://localhost:8080/api/v1/auth/verify");
        ReflectionTestUtils.setField(emailService, "resetPasswordUrl", "http://localhost:8080/api/v1/auth/reset-password");
    }

    @Test
    void sendVerificationEmailBuildsExpectedMessage() {
        emailService.sendVerificationEmail("user@test.com", "token value");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();

        assertThat(message.getFrom()).isEqualTo("ProjectM <no-reply@test.com>");
        assertThat(message.getTo()).containsExactly("user@test.com");
        assertThat(message.getSubject()).isEqualTo("Verify your ProjectM account");
        assertThat(message.getText()).contains("http://localhost:8080/api/v1/auth/verify?token=token+value");
    }

    @Test
    void sendPasswordResetEmailBuildsExpectedMessage() {
        emailService.sendPasswordResetEmail("user@test.com", "reset token");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();

        assertThat(message.getFrom()).isEqualTo("ProjectM <no-reply@test.com>");
        assertThat(message.getTo()).containsExactly("user@test.com");
        assertThat(message.getSubject()).isEqualTo("Reset your password");
        assertThat(message.getText()).contains("http://localhost:8080/api/v1/auth/reset-password?token=reset+token");
    }
}
