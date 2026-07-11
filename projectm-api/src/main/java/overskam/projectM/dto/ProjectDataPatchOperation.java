package overskam.projectM.dto;

import jakarta.validation.constraints.NotBlank;

public record ProjectDataPatchOperation (
        @NotBlank(message = "Operation must exist")
        String op,
        
        @NotBlank@NotBlank(message = "Path must exist")
        String path,
        
        Object value
) {
}
