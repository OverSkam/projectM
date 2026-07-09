package overskam.projectM.dto;

import overskam.projectM.enums.BuildStatus;

import java.util.UUID;

public record BuildResponse (
        UUID id,
        String projectId,
        BuildStatus status,
        String artifactKey,
        String errorMessage
) {
}
