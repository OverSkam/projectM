package overskam.projectM.dto;

import java.util.UUID;

public record BuildRequestMessage (
        UUID buildId,
        String projectId,
        UUID ownerId
) {
}
