package overskam.projectM.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import overskam.projectM.model.PluginBuild;

import java.util.UUID;

public interface PluginBuildRepository extends JpaRepository<PluginBuild, UUID> {
}
