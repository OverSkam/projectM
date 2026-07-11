package overskam.projectM.common.dto;

import java.util.UUID;

public record CompileTaskMessage(
        UUID buildId,
        String projectId,
        UUID ownerId,
        String replyTo
) {
}
