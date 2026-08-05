package overskam.projectM.model;

import org.springframework.data.annotation.Id;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Document(collection = "projects")
@CompoundIndexes({
    @CompoundIndex(name = "idx_owner_name",    def = "{'ownerId': 1, 'name': 1}"),
    @CompoundIndex(name = "idx_owner_updated", def = "{'ownerId': 1, 'lastModifiedAt': -1}")
})
public class Project {
    @Id
    private String id;
    
    private UUID ownerId;
    private String name;
    private Map<String, Object> projectData;
    
    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime lastModifiedAt;
}
