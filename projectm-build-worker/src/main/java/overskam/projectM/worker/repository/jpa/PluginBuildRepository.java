package overskam.projectM.worker.repository.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import overskam.projectM.worker.model.PluginBuild;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PluginBuildRepository extends JpaRepository<PluginBuild, UUID> {
    Page<PluginBuild> findByProjectIdAndOwnerId(String projectId, UUID ownerId, Pageable pageable);
    Optional<PluginBuild> findById(UUID buildId);
    List<PluginBuild> findByProjectId(String projectId);
}
