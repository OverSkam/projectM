package overskam.projectM.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

public record PasswordResetRequest(
        @NotBlank(message = "Password is required")
        @Length(min = 8, max = 200,
                message = "Password should be at least 8 characters"
        )
        String password,
        
        @NotBlank
        String token
) {
}
