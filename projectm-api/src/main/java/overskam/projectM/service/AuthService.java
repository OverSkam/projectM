package overskam.projectM.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import overskam.projectM.dto.EmailRequest;
import overskam.projectM.dto.LoginRequest;
import overskam.projectM.dto.PasswordResetRequest;
import overskam.projectM.dto.RegisterRequest;
import overskam.projectM.enums.TokenType;
import overskam.projectM.exception.InvalidRequestException;
import overskam.projectM.model.CustomUserDetails;
import overskam.projectM.model.User;
import overskam.projectM.model.VerificationToken;
import overskam.projectM.repository.jpa.UserRepository;
import overskam.projectM.repository.jpa.VerificationTokenRepository;
import overskam.projectM.util.JwtUtil;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class AuthService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final EmailService emailService;
    private final TokenVersionService tokenVersionService;
    private final CustomUserDetailsService userDetailsService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    
    @Transactional
    public String login(LoginRequest loginRequest) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.email());
        User user = ((CustomUserDetails) userDetails).getUser();
        
        if (!user.getEnabled())
            throw new DisabledException("Please verify your email");
        
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(userDetails.getUsername(), loginRequest.password())
        );
        return jwtUtil.generateToken(userDetails.getUsername(), user.getId(), user.getTokenVersion());
    }

    @Transactional
    public void register(RegisterRequest registerRequest) {
        if (userRepository.findByEmail(registerRequest.email()).isPresent())
            throw new InvalidRequestException("Invalid email address!");

        User user = new User();
        user.setEmail(registerRequest.email());
        user.setEnabled(false);
        user.setPassword(passwordEncoder.encode(registerRequest.password()));
        userRepository.save(user);
        log.info("User with id: {} was registered", user.getId());

        VerificationToken token = createNewTokenForUser(user, TokenType.EMAIL_VERIFICATION);

        emailService.sendVerificationEmail(user.getEmail(), token.getToken());
    }
    
    @Transactional
    public void verify(String token) {
        VerificationToken verificationToken = tokenRepository.findByTokenAndType(token, TokenType.EMAIL_VERIFICATION)
                .orElseThrow(() -> new InvalidRequestException("Invalid token"));
        
        if (verificationToken.getTokenExpiresAt().isBefore(LocalDateTime.now()))
            throw new InvalidRequestException("Token expired");
        
        verificationToken.getUser().setEnabled(true);
        tokenRepository.delete(verificationToken);
        log.info("User with id: {} was verified", verificationToken.getUser().getId());
    }
    
    @Transactional
    public void resendVerification(EmailRequest emailRequest) {
        String email = emailRequest.email();
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent() && !user.get().getEnabled()) {
            log.info("Unverified user with id: {} requesting new verification email", user.get().getId());
            VerificationToken verificationToken = createNewTokenForUser(user.get(), TokenType.EMAIL_VERIFICATION);
            
            emailService.sendVerificationEmail(user.get().getEmail(), verificationToken.getToken());
        } else
            log.info("User doesn't exist or already verified");
    }
    
    @Transactional
    public void requestPasswordReset(EmailRequest emailRequest) {
        String email = emailRequest.email();
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            log.info("User with id: {} trying to reset his password", user.get().getId());
            VerificationToken verificationToken = createNewTokenForUser(user.get(), TokenType.PASSWORD_RESET);
            
            emailService.sendPasswordResetEmail(user.get().getEmail(), verificationToken.getToken());
        } else
            log.info("User doesn't exist");
    }
    
    @Transactional
    public void resetPassword(PasswordResetRequest passwordResetRequest) {
        VerificationToken verificationToken = tokenRepository.findByTokenAndType(passwordResetRequest.token(), TokenType.PASSWORD_RESET)
                .orElseThrow(() -> new InvalidRequestException("Invalid token"));
        
        if (verificationToken.getTokenExpiresAt().isBefore(LocalDateTime.now()))
            throw new InvalidRequestException("Token expired");
        
        User user = verificationToken.getUser();
        user.setPassword(passwordEncoder.encode(passwordResetRequest.password()));
        tokenRepository.delete(verificationToken);
        tokenVersionService.bumpVersion(user.getId());
        log.info("User with id: {} has changed his password", user.getId());
    }
    
    private VerificationToken createNewTokenForUser(User user, TokenType type) {
        VerificationToken currentToken = tokenRepository.findByUserAndType(user, type)
                .orElse(new VerificationToken());
        currentToken.setToken(UUID.randomUUID().toString());
        currentToken.setTokenExpiresAt(LocalDateTime.now().plusMinutes(30));
        currentToken.setUser(user);
        currentToken.setType(type);
        return tokenRepository.save(currentToken);
    }

}
