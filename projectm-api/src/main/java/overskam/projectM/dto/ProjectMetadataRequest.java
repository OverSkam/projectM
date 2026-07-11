package overskam.projectM.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;
import overskam.projectM.validation.FullUpdate;

public record ProjectMetadataRequest(
        @NotBlank(message = "Project name must exist", groups = FullUpdate.class)
        @Length(max = 40)
        String name
) {
}
