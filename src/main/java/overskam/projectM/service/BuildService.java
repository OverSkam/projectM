package overskam.projectM.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import overskam.projectM.config.RabbitMqConfig;
import overskam.projectM.dto.CompileTaskMessage;
import overskam.projectM.exception.InvalidRequestException;
import overskam.projectM.model.PluginBuild;
import overskam.projectM.model.User;
import overskam.projectM.repository.jpa.PluginBuildRepository;
import overskam.projectM.repository.mongo.ProjectRepository;

import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class BuildService {
    private final BuildQueuePublisher buildQueuePublisher;
    private final ProjectRepository projectRepository;
    private final PluginBuildRepository buildRepository;
    
    @Transactional
    public UUID buildProject(User user, String projectId) {
        if (!projectRepository.existsByIdAndOwnerId(projectId, user.getId()))
            throw new InvalidRequestException("Project doesn't exist or user doesn't have ownership");
        
        PluginBuild build = new PluginBuild();
        build.setProjectId(projectId);
        build.setOwnerId(user.getId());
        buildRepository.save(build);
        
        buildQueuePublisher.publish(new CompileTaskMessage(
                build.getId(), projectId, user.getId(), RabbitMqConfig.RPC_REPLY_QUEUE
        ));
        log.info("Compile task was published successfully");
        return build.getId();
    }
}
