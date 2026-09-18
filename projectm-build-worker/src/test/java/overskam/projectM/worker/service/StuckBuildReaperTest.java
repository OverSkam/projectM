package overskam.projectM.worker.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import overskam.projectM.common.enums.BuildStatus;
import overskam.projectM.worker.AbstractWorkerIntegrationTest;
import overskam.projectM.worker.model.PluginBuild;
import overskam.projectM.worker.repository.jpa.PluginBuildRepository;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(StuckBuildReaper.class)
class StuckBuildReaperTest extends AbstractWorkerIntegrationTest {
    
    @Autowired
    private StuckBuildReaper reaper;
    
    @Autowired
    private PluginBuildRepository pluginBuildRepository;
    
    @Test
    @DisplayName("Fails stale running or queued builds")
    void failsStaleBuilds() {
        String stuckRunning = saveBuild(BuildStatus.RUNNING, 90);
        String freshRunning = saveBuild(BuildStatus.RUNNING, 10);
        String stuckQueued  = saveBuild(BuildStatus.QUEUED, 45);
        String freshQueued  = saveBuild(BuildStatus.QUEUED, 5);
        
        reaper.failStuckBuilds();
        
        PluginBuild firstUpdate = pluginBuildRepository.findByProjectId(stuckRunning).getFirst();
        assertEquals(BuildStatus.FAILED, firstUpdate.getStatus());
        assertEquals("Build timed out", firstUpdate.getErrorMessage());
        assertEquals(BuildStatus.RUNNING, pluginBuildRepository.findByProjectId(freshRunning).getFirst().getStatus());
        PluginBuild thirdUpdate = pluginBuildRepository.findByProjectId(stuckQueued).getFirst();
        assertEquals(BuildStatus.FAILED,thirdUpdate.getStatus());
        assertEquals("Build was never picked up", thirdUpdate.getErrorMessage());
        assertEquals(BuildStatus.QUEUED, pluginBuildRepository.findByProjectId(freshQueued).getFirst().getStatus());
    }
    
    private String saveBuild(BuildStatus status, int minutesAgo) {
        String projectId = UUID.randomUUID().toString();
        PluginBuild build = new PluginBuild();
        build.setOwnerId(UUID.randomUUID());
        build.setProjectId(projectId);
        build.setStatus(status);
        build.setCreateDate(LocalDateTime.now().minusMinutes(minutesAgo));
        build.setLastModifiedDate(LocalDateTime.now().minusMinutes(minutesAgo));
        pluginBuildRepository.save(build);
        return projectId;
    }
}
