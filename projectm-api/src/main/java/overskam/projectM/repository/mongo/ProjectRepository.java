package overskam.projectM.repository.mongo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import overskam.projectM.model.Project;

import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends MongoRepository<Project, String> {
    Optional<Project> findById(String projectId);
    boolean existsByIdAndOwnerId(String projectId, UUID userId);
    
    @Query(value = "{ 'ownerId': ?0 }", fields = "{ 'name': 1 }")
    Page<ProjectNameView> findProjectNamesByOwnerId(UUID ownerId, Pageable pageable);
    
    interface ProjectNameView {
        String getId();
        String getName();
    }
}
