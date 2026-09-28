package overskam.projectM.dto;

import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;

public record ProjectMetadataRequest(
        @Pattern(regexp = "(?=.*[A-Za-z0-9])[A-Za-z0-9 _.-]+",
                message = "Project name may contain only letters, digits, spaces, dots, hyphens and underscores")
        @Length(min = 1, max = 40, message = "Project name must be 1-40 characters")
        String name
) {
}
