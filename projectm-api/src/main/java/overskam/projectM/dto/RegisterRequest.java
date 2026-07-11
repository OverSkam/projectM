package overskam.projectM.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;
import overskam.projectM.validation.FullUpdate;
import overskam.projectM.validation.PartialUpdate;

public record RegisterRequest (
        @NotBlank(message = "Email is required", groups = {FullUpdate.class})
        @Email(message = "Email should be valid", groups = {FullUpdate.class, PartialUpdate.class})
        String email,

        @NotBlank(message = "Password is required")
        @Length(min = 8, max = 200,
                message = "Password should be at least 8 characters"
        )
        String password
) {}
