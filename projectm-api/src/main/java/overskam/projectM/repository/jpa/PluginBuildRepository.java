package overskam.projectM.repository.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import overskam.projectM.common.enums.BuildStatus;
import overskam.projectM.model.PluginBuild;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface PluginBuildRepository extends JpaRepository<PluginBuild, UUID> {
    Page<PluginBuild> findByProjectIdAndOwnerId(String projectId, UUID ownerId, Pageable pageable);
    Optional<PluginBuild> findById(UUID buildId);
    boolean existsByProjectIdAndStatusIn(String projectId, Collection<BuildStatus> statuses);
}
