package overskam.projectM.repository.mongo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import overskam.projectM.model.Project;

import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends MongoRepository<Project, String> {
    Page<Project> findByOwnerId(UUID ownerId, Pageable pageable);
    Optional<Project> findById(String projectId);
    Optional<Project> findByIdAndOwnerId(String projectId, UUID ownerId);
    boolean existsByIdAndOwnerId(String projectId, UUID userId);
}
