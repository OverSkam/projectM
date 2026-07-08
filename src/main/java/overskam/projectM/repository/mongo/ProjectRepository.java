package overskam.projectM.repository.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import overskam.projectM.dto.ProjectNameResponse;
import overskam.projectM.model.Project;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends MongoRepository<Project, String> {
    List<ProjectNameResponse> findByOwnerId(UUID ownerId);
    Optional<Project> findById(String projectId);
}
