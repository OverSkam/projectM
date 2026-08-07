package overskam.projectM.worker.listener;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plummy.visualcore.elements.Plugin;
import org.plummy.visualcore.exceptions.compilation.InvalidSourcesException;
import org.plummy.visualcore.exceptions.validation.data.DataValidationException;
import org.plummy.visualcore.reports.Report;
import org.plummy.visualcore.tools.PluginCompiler;
import org.plummy.visualcore.tools.PluginValidator;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import overskam.projectM.common.dto.CompileTaskMessage;
import overskam.projectM.common.mq.RabbitMqNames;
import overskam.projectM.worker.model.Project;
import overskam.projectM.worker.repository.mongo.ProjectRepository;
import overskam.projectM.worker.service.ArtifactStorageService;
import overskam.projectM.worker.service.WorkerPluginBuildService;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Component
@AllArgsConstructor
public class CompileTaskListener {
    private final ProjectRepository projectRepository;
    private final WorkerPluginBuildService pluginBuildService;
    private final PluginCompiler pluginCompiler;
    private final ArtifactStorageService artifactStorageService;
    private final PluginValidator pluginValidator;
    private final ObjectMapper objectMapper;
    
    @RabbitListener(queues = RabbitMqNames.COMPILE_TASK_QUEUE)
    public void handleCompileTask(CompileTaskMessage message) throws IOException {
        log.info("Received compile task: {}", message);
        
        if (!pluginBuildService.claim(message.buildId())) {
            log.info("Build already claimed or finished, skipping. buildId={}", message.buildId());
            return;
        }
        try {
        Project project = projectRepository.findByIdAndOwnerId(message.projectId(), message.ownerId())
                .orElse(null);
        
        if (project == null) {
            pluginBuildService.markFailed(message.buildId(), "Project no longer exists");
            return;
        }
        
        Plugin plugin;
        try {
            plugin = Plugin.fromMap(project.getProjectData());
        } catch (RuntimeException e) {
            log.error("Project data is malformed, buildId: {}, projectId: {}", message.buildId(), project.getId(), e);
            pluginBuildService.markFailed(message.buildId(), "Project data is malformed");
            return;
        }
        
        var reports = pluginValidator.collectPluginPreCompileReports(plugin);
        if (!reports.isEmpty()) {
            var details = reports.stream().map(Report::toMap).toList();
            pluginBuildService.markFailed(message.buildId(), objectMapper.writeValueAsString(details));
            return;
        }
        
        byte[] jar;
        try {
            jar = pluginCompiler.compilePlugin(plugin, project.getName(), "1.0.0", "ProjectM");
        } catch (DataValidationException | InvalidSourcesException e) {
            log.error("Compile failed build: {}, projectId: {}", message.buildId(), project.getId(), e);
            pluginBuildService.markFailed(message.buildId(), e.getMessage());
            return;
        }
       
        String artifactKey = artifactStorageService.saveJar(message.buildId(), jar);
        pluginBuildService.markSuccess(message.buildId(), artifactKey);
        } catch (IOException | RuntimeException e) {
            pluginBuildService.release(message.buildId());
            throw e;
        }
    }
}