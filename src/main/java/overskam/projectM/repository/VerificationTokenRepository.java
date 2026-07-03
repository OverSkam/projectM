package overskam.projectM.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import overskam.projectM.enums.TokenType;
import overskam.projectM.model.User;
import overskam.projectM.model.VerificationToken;

import java.util.Optional;
import java.util.UUID;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {
    Optional<VerificationToken> findByUserAndType(User user, TokenType type);
    Optional<VerificationToken> findByTokenAndType(String token, TokenType type);
}
