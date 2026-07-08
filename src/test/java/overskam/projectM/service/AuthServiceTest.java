package overskam.projectM.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import overskam.projectM.dto.EmailRequest;
import overskam.projectM.dto.PasswordResetRequest;
import overskam.projectM.dto.RegisterRequest;
import overskam.projectM.enums.TokenType;
import overskam.projectM.exception.InvalidRequestException;
import overskam.projectM.model.User;
import overskam.projectM.model.VerificationToken;
import overskam.projectM.repository.jpa.UserRepository;
import overskam.projectM.repository.jpa.VerificationTokenRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    private static final String EMAIL = "user@test.com";
    private static final String PASSWORD = "password123";
    private static final String ENCODED_PASSWORD = "encoded-password";
    private static final String TOKEN = "token";

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VerificationTokenRepository tokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private TokenVersionService tokenVersionService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerCreatesDisabledUserVerificationTokenAndSendsOneEmail() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenRepository.findByUserAndType(any(User.class), eq(TokenType.EMAIL_VERIFICATION)))
                .thenReturn(Optional.empty());
        when(tokenRepository.save(any(VerificationToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.register(new RegisterRequest(EMAIL, PASSWORD));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo(EMAIL);
        assertThat(savedUser.getPassword()).isEqualTo(ENCODED_PASSWORD);
        assertThat(savedUser.getEnabled()).isFalse();

        ArgumentCaptor<VerificationToken> tokenCaptor = ArgumentCaptor.forClass(VerificationToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        VerificationToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getToken()).isNotBlank();
        assertThat(savedToken.getType()).isEqualTo(TokenType.EMAIL_VERIFICATION);
        assertThat(savedToken.getUser()).isSameAs(savedUser);
        assertThat(savedToken.getTokenExpiresAt()).isAfter(LocalDateTime.now());

        verify(emailService, times(1)).sendVerificationEmail(eq(EMAIL), eq(savedToken.getToken()));
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> authService.register(new RegisterRequest(EMAIL, PASSWORD)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Invalid email address!");

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(tokenRepository, emailService);
    }

    @Test
    void verifyEnablesUserAndDeletesToken() {
        User user = user(false, ENCODED_PASSWORD);
        VerificationToken token = token(user, TokenType.EMAIL_VERIFICATION, LocalDateTime.now().plusMinutes(5));
        when(tokenRepository.findByTokenAndType(TOKEN, TokenType.EMAIL_VERIFICATION)).thenReturn(Optional.of(token));

        authService.verify(TOKEN);

        assertThat(user.getEnabled()).isTrue();
        verify(tokenRepository).delete(token);
    }

    @Test
    void verifyRejectsExpiredToken() {
        User user = user(false, ENCODED_PASSWORD);
        VerificationToken token = token(user, TokenType.EMAIL_VERIFICATION, LocalDateTime.now().minusSeconds(1));
        when(tokenRepository.findByTokenAndType(TOKEN, TokenType.EMAIL_VERIFICATION)).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.verify(TOKEN))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Token expired");

        assertThat(user.getEnabled()).isFalse();
        verify(tokenRepository, never()).delete(any(VerificationToken.class));
    }

    @Test
    void resendVerificationSendsOneEmailForExistingUnverifiedUser() {
        User user = user(false, ENCODED_PASSWORD);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(tokenRepository.findByUserAndType(user, TokenType.EMAIL_VERIFICATION)).thenReturn(Optional.empty());
        when(tokenRepository.save(any(VerificationToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.resendVerification(new EmailRequest(EMAIL));

        ArgumentCaptor<VerificationToken> tokenCaptor = ArgumentCaptor.forClass(VerificationToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        verify(emailService, times(1)).sendVerificationEmail(EMAIL, tokenCaptor.getValue().getToken());
    }

    @Test
    void resendVerificationDoesNothingForMissingUser() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        authService.resendVerification(new EmailRequest(EMAIL));

        verifyNoInteractions(tokenRepository, emailService);
    }

    @Test
    void requestPasswordResetSendsOnePasswordResetEmailForExistingUser() {
        User user = user(true, ENCODED_PASSWORD);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(tokenRepository.findByUserAndType(user, TokenType.PASSWORD_RESET)).thenReturn(Optional.empty());
        when(tokenRepository.save(any(VerificationToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.requestPasswordReset(new EmailRequest(EMAIL));

        ArgumentCaptor<VerificationToken> tokenCaptor = ArgumentCaptor.forClass(VerificationToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        VerificationToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getType()).isEqualTo(TokenType.PASSWORD_RESET);
        verify(emailService, times(1)).sendPasswordResetEmail(EMAIL, savedToken.getToken());
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void resetPasswordChangesPasswordDeletesTokenAndRevokesSessions() {
        User user = user(true, "old-password");
        UUID userId = UUID.randomUUID();
        user.setId(userId);
        VerificationToken token = token(user, TokenType.PASSWORD_RESET, LocalDateTime.now().plusMinutes(5));
        when(tokenRepository.findByTokenAndType(TOKEN, TokenType.PASSWORD_RESET)).thenReturn(Optional.of(token));
        when(passwordEncoder.encode(PASSWORD)).thenReturn(ENCODED_PASSWORD);

        authService.resetPassword(new PasswordResetRequest(PASSWORD, TOKEN));

        assertThat(user.getPassword()).isEqualTo(ENCODED_PASSWORD);
        verify(tokenRepository).delete(token);
        verify(tokenVersionService).bumpVersion(userId);
    }

    @Test
    void resetPasswordRejectsExpiredToken() {
        User user = user(true, "old-password");
        VerificationToken token = token(user, TokenType.PASSWORD_RESET, LocalDateTime.now().minusSeconds(1));
        when(tokenRepository.findByTokenAndType(TOKEN, TokenType.PASSWORD_RESET)).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.resetPassword(new PasswordResetRequest(PASSWORD, TOKEN)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Token expired");

        verify(tokenRepository, never()).delete(any(VerificationToken.class));
        verifyNoInteractions(tokenVersionService);
    }

    private static User user(boolean enabled, String password) {
        User user = new User();
        user.setEmail(EMAIL);
        user.setEnabled(enabled);
        user.setPassword(password);
        user.setTokenVersion(0L);
        return user;
    }

    private static VerificationToken token(User user, TokenType type, LocalDateTime expiresAt) {
        VerificationToken token = new VerificationToken();
        token.setToken(TOKEN);
        token.setUser(user);
        token.setType(type);
        token.setTokenExpiresAt(expiresAt);
        return token;
    }
}
