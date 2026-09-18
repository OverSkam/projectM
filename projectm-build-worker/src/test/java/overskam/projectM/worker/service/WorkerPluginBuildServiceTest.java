package overskam.projectM.worker.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import overskam.projectM.common.enums.BuildStatus;
import overskam.projectM.worker.AbstractWorkerIntegrationTest;
import overskam.projectM.worker.config.JpaAuditingConfig;
import overskam.projectM.worker.model.PluginBuild;
import overskam.projectM.worker.repository.jpa.PluginBuildRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({JpaAuditingConfig.class, WorkerPluginBuildService.class})
class WorkerPluginBuildServiceTest extends AbstractWorkerIntegrationTest {
    
    @Autowired
    private PluginBuildRepository pluginBuildRepository;
    
    @Autowired
    private WorkerPluginBuildService pluginBuildService;
  
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("Only one worker can claim the build")
    void onlyOneWorkerCanClaimBuild() throws Exception {
        UUID ownerId = UUID.randomUUID();
        String projectId = UUID.randomUUID().toString();
        PluginBuild build = new PluginBuild();
        build.setOwnerId(ownerId);
        build.setProjectId(projectId);
        build.setStatus(BuildStatus.QUEUED);
        UUID buildId = pluginBuildRepository.save(build).getId();
        
        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        
        List<Future<Boolean>> futures = new ArrayList<>();
        
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                startGate.await();
                return pluginBuildService.claim(buildId);
            }));
        }
        
        startGate.countDown();
        
        int claimed = 0;
        for (Future<Boolean> future : futures) {
            if (future.get(30, TimeUnit.SECONDS)) {
                claimed++;
            }
        }
        
        pool.shutdown();
        
        assertEquals(1, claimed);
        assertEquals(BuildStatus.RUNNING, pluginBuildRepository.findByProjectId(projectId).getFirst().getStatus());
    }
}
