package overskam.projectM.dto;

import java.util.Map;

public record ReplaceProjectDataRequest(
        Map<String, Object> projectData
) {
}
