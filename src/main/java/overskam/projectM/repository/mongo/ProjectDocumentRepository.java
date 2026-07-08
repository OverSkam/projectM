package overskam.projectM.repository.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import overskam.projectM.model.ProjectDocument;

import java.util.List;
import java.util.UUID;

public interface ProjectDocumentRepository extends MongoRepository<ProjectDocument, String> {
    List<ProjectDocument> findByOwnerId(UUID ownerId);
}
