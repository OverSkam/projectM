package overskam.projectM.worker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import overskam.projectM.common.enums.BuildStatus;
import overskam.projectM.worker.repository.jpa.PluginBuildRepository;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class StuckBuildReaper {
    private final PluginBuildRepository pluginBuildRepository;
    
    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
    @Transactional
    public void failStuckBuilds() {
        int running = pluginBuildRepository.failStale(BuildStatus.RUNNING, BuildStatus.FAILED,
                "Build timed out", LocalDateTime.now().minusMinutes(60));
        int queued = pluginBuildRepository.failStale(BuildStatus.QUEUED, BuildStatus.FAILED,
                "Build was never picked up", LocalDateTime.now().minusMinutes(30));
        
        if (running + queued > 0)
            log.warn("Reaped {} stuck running and {} stuck queued builds", running, queued);
    }
}
