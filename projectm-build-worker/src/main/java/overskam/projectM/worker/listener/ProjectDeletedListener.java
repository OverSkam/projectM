package overskam.projectM.worker.listener;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import overskam.projectM.common.dto.ProjectDeletedMessage;
import overskam.projectM.common.mq.RabbitMqNames;
import overskam.projectM.worker.model.PluginBuild;
import overskam.projectM.worker.repository.jpa.PluginBuildRepository;
import overskam.projectM.worker.service.ArtifactStorageService;

import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class ProjectDeletedListener {
    private final PluginBuildRepository buildRepository;
    private final ArtifactStorageService artifactStorageService;
    
    @RabbitListener(queues = RabbitMqNames.CLEANUP_QUEUE)
    @Transactional
    public void onProjectDeleted(ProjectDeletedMessage message) {
        log.info("Cleaning up deleted project, projectId={}, ownerId={}",
                message.projectId(), message.ownerId());
        
        List<PluginBuild> builds = buildRepository.findByProjectId(message.projectId());
        for (PluginBuild build : builds) {
            if (build.getArtifactKey() != null)
                artifactStorageService.deleteByKey(build.getArtifactKey());
        }
        buildRepository.deleteAll(builds);
        
        log.info("Cleaned up {} build(s) for projectId={}", builds.size(), message.projectId());
    }
}
