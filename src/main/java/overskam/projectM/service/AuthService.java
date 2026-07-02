package overskam.projectM.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import overskam.projectM.dto.RegisterRequest;
import overskam.projectM.exception.InvalidRequestException;
import overskam.projectM.model.User;
import overskam.projectM.repository.UserRepository;

@Slf4j
@Service
@AllArgsConstructor
public class AuthService {
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    private final UserRepository userRepository;

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

//        VerificationToken token = createNewTokenForUser(user, TokenType.EMAIL_VERIFICATION);
//        tokenRepository.save(token);
//        log.info("Verification token for user with id: {} was created", user.getId());

//        emailService.sendVerificationEmail(user.getEmail(), token.getToken());
    }


}
