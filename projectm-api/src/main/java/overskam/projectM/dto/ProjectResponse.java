package overskam.projectM.dto;

import java.util.Map;

public record ProjectResponse (
        String id,
        String name,
        Map<String, Object> projectData
) {
}
