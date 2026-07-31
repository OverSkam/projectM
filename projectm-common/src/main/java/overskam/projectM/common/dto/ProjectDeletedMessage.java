 package overskam.projectM.common.dto;
 
import java.util.UUID;

public record ProjectDeletedMessage (
        String projectId,
          UUID ownerId
) {}