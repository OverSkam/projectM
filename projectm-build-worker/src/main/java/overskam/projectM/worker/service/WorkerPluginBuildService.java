package overskam.projectM.worker.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import overskam.projectM.common.enums.BuildStatus;
import overskam.projectM.worker.model.PluginBuild;
import overskam.projectM.worker.repository.jpa.PluginBuildRepository;

import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class WorkerPluginBuildService {
    private final PluginBuildRepository buildRepository;
    
    @Transactional
    public void markRunning(UUID buildId) {
        PluginBuild build = buildRepository.findById(buildId)
                .orElseThrow(() -> new IllegalArgumentException("Build not found"));
        build.setStatus(BuildStatus.RUNNING);
        buildRepository.save(build);
    }
    
    @Transactional
    public void markSuccess(UUID buildId, String artifactKey) {
        PluginBuild build = buildRepository.findById(buildId)
                .orElseThrow(() -> new IllegalArgumentException("Build not found"));
        build.setStatus(BuildStatus.SUCCESS);
        build.setArtifactKey(artifactKey);
        buildRepository.save(build);
    }
    
    @Transactional
    public void markFailed(UUID buildId, String errorMessage) {
        PluginBuild build = buildRepository.findById(buildId)
                .orElseThrow(() -> new IllegalArgumentException("Build not found"));
        build.setStatus(BuildStatus.FAILED);
        build.setErrorMessage(errorMessage);
        buildRepository.save(build);
    }
}
