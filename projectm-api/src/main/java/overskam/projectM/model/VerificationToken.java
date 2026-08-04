package overskam.projectM.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import overskam.projectM.enums.TokenType;

import java.time.LocalDateTime;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@Table(name = "verification_tokens")
public class VerificationToken extends AbstractModel{
    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime tokenExpiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, columnDefinition = "VARCHAR(20)")
    private TokenType type;
}
