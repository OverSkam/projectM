package overskam.projectM.worker.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import overskam.projectM.common.enums.BuildStatus;
import overskam.projectM.worker.repository.jpa.PluginBuildRepository;

import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class WorkerPluginBuildService {
    private final PluginBuildRepository buildRepository;
    
    private static final int MAX_ERROR_LENGTH = 2000;
    
    @Transactional
    public boolean claim(UUID buildId) {
        return buildRepository.updateStatusIfCurrent(buildId, BuildStatus.QUEUED, BuildStatus.RUNNING) == 1;
    }
    
    @Transactional
    public void release(UUID buildId) {
        buildRepository.updateStatusIfCurrent(buildId, BuildStatus.RUNNING, BuildStatus.QUEUED);
    }
    
    @Transactional
    public boolean markSuccess(UUID buildId, String artifactKey) {
        int updated = buildRepository.finishIfCurrent(
                buildId, BuildStatus.RUNNING, BuildStatus.SUCCESS, artifactKey, null);
        if (updated == 0)
            log.warn("Build {} was no longer RUNNING at completion; discarding artifact {}", buildId, artifactKey);
        return updated == 1;
    }
    
    @Transactional
    public void markFailed(UUID buildId, String errorMessage) {
        int updated = buildRepository.finishIfCurrent(
                buildId, BuildStatus.RUNNING, BuildStatus.FAILED, null, truncate(errorMessage));
        if (updated == 0)
            log.warn("Build {} was no longer RUNNING at failure; result discarded", buildId);
    }
    
    private static String truncate(String s) {
        if (s == null ||  s.length() <  MAX_ERROR_LENGTH) return s;
        return s.substring(0, MAX_ERROR_LENGTH - 3) + "...";
    }
}
