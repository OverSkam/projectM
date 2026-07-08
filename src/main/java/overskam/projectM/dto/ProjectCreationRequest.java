package overskam.projectM.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

public record ProjectCreationRequest (
        @NotBlank(message = "Project name must exist")
        @Length(max = 40)
        String name
) {
}
