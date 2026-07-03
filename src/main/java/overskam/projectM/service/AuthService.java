package overskam.projectM.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import overskam.projectM.dto.RegisterRequest;
import overskam.projectM.enums.TokenType;
import overskam.projectM.exception.InvalidRequestException;
import overskam.projectM.model.User;
import overskam.projectM.model.VerificationToken;
import overskam.projectM.repository.UserRepository;
import overskam.projectM.repository.VerificationTokenRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class AuthService {
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;

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
        tokenRepository.save(token);
        log.info("Verification token for user with id: {} was created", user.getId());

//        emailService.sendVerificationEmail(user.getEmail(), token.getToken());
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
    
    private VerificationToken createNewTokenForUser(User user, TokenType type) {
        VerificationToken currentToken = tokenRepository.findByUserAndType(user, type)
                .orElse(new VerificationToken());
        currentToken.setToken(UUID.randomUUID().toString());
        currentToken.setTokenExpiresAt(LocalDateTime.now().plusMinutes(30));
        currentToken.setUser(user);
        currentToken.setType(type);
        tokenRepository.save(currentToken);
        // tmp:
        log.info("Token: {}", currentToken);
        return currentToken;
    }

}
