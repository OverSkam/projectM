package overskam.projectM.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;

public record ProjectCreationRequest (
        
        @Pattern(regexp = "(?=.*[A-Za-z0-9])[A-Za-z0-9 _.-]+",
                message = "Project name may contain only letters, digits, spaces, dots, hyphens and underscores")
        @NotBlank(message = "Project name must exist")
        @Length(max = 40)
        String name
) {
}
