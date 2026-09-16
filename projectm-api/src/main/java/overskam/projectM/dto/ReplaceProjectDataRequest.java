package overskam.projectM.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record ReplaceProjectDataRequest(
        @NotNull(message = "Project data is required")
        Map<String, Object> projectData
) {
}
