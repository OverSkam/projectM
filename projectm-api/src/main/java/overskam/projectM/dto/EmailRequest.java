package overskam.projectM.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import overskam.projectM.validation.FullUpdate;
import overskam.projectM.validation.PartialUpdate;

public record EmailRequest(
        @NotBlank(message = "Email is required", groups = {FullUpdate.class})
        @Email(message = "Email should be valid", groups = {FullUpdate.class, PartialUpdate.class})
        String email
) { }
