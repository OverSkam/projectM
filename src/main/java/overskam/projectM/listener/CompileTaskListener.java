package overskam.projectM.listener;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plummy.visualcore.elements.Plugin;
import org.plummy.visualcore.exceptions.validation.data.DataValidationException;
import org.plummy.visualcore.tools.PluginCompiler;
import org.plummy.visualcore.tools.PluginValidator;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import overskam.projectM.config.RabbitMqConfig;
import overskam.projectM.dto.CompileTaskMessage;
import overskam.projectM.exception.NotFoundException;
import overskam.projectM.model.Project;
import overskam.projectM.repository.mongo.ProjectRepository;
import overskam.projectM.service.ArtifactStorageService;
import overskam.projectM.service.PluginBuildService;

import java.io.IOException;

@Slf4j
@Component
@AllArgsConstructor
public class CompileTaskListener {
    private final PluginBuildService pluginBuildService;
    private final ProjectRepository projectRepository;
    private final PluginCompiler pluginCompiler;
    private final ArtifactStorageService artifactStorageService;
    
    @RabbitListener(queues = RabbitMqConfig.COMPILE_TASK_QUEUE)
    public void handleCompileTask(CompileTaskMessage message) {
        log.info("Received compile task: {}", message);
        
//        pluginBuildService.markRunning(message.buildId());
        try {
            Project project = projectRepository.findByIdAndOwnerId(message.projectId(), message.ownerId())
                    .orElseThrow(() -> new NotFoundException("Project not found"));
            
            Plugin plugin = Plugin.fromMap(project.getProjectData());
            PluginValidator validator = new PluginValidator();
            validator.validatePluginPreCompile(plugin);
            
            byte[] jar = pluginCompiler.compilePlugin(
                    plugin,
                    project.getName(),
                    "1.0.0",
                    "ProjectM"
            );
            
            String artifactKey = artifactStorageService.saveJar(message.buildId(), jar);
            pluginBuildService.markSuccess(message.buildId(), artifactKey);
        } catch (RuntimeException | IOException e) {
            log.error("Exception: ", e);
            pluginBuildService.markFailed(message.buildId(), e.getMessage());
        }
    }
}
