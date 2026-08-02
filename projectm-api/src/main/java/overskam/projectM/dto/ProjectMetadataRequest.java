package overskam.projectM.dto;

import org.hibernate.validator.constraints.Length;

public record ProjectMetadataRequest(
        @Length(min = 1, max = 40, message = "Project name must be 1-40 characters")
        String name
) {
}
