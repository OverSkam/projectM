package overskam.projectM.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record ReplaceProjectDataRequest(
        @NotNull
        Map<String, Object> projectData
) {
}
