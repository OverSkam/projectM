package overskam.projectM.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import overskam.projectM.config.RabbitMqConfig;
import overskam.projectM.dto.ArtifactResponse;
import overskam.projectM.dto.BuildResponse;
import overskam.projectM.dto.CompileTaskMessage;
import overskam.projectM.enums.BuildStatus;
import overskam.projectM.exception.InvalidRequestException;
import overskam.projectM.exception.NotFoundException;
import overskam.projectM.exception.OwnershipException;
import overskam.projectM.model.PluginBuild;
import overskam.projectM.model.User;
import overskam.projectM.repository.jpa.PluginBuildRepository;
import overskam.projectM.repository.mongo.ProjectRepository;
import overskam.projectM.util.SortingUtil;

import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class PluginBuildService {
    private final BuildQueuePublisher buildQueuePublisher;
    private final ProjectRepository projectRepository;
    private final PluginBuildRepository buildRepository;
    
    public Page<BuildResponse> getBuilds(User user, String projectId, int page, int size, String sortBy, String sortDirection) {
        Sort sort = SortingUtil.sortGenerator(sortBy, sortDirection);
        log.info("Builds for project with id: {} was fetched successfully", projectId);
        return buildRepository.findByProjectIdAndOwnerId(
                        projectId, user.getId(), PageRequest.of(page, size, sort))
                .map(build ->
                        new BuildResponse(
                                build.getId(),
                                build.getProjectId(),
                                build.getStatus(),
                                build.getArtifactKey(),
                                build.getErrorMessage()
                        )
                );
    }
    
    public BuildResponse getBuild(User user, String projectId, UUID buildId) {
        PluginBuild build = fetchOrThrow(user.getId(), projectId, buildId);
        log.info("Build with id: {} was fetched successfully", buildId);
        
        return new BuildResponse(build.getId(), build.getProjectId(), build.getStatus(), build.getArtifactKey(), build.getErrorMessage());
    }
    
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
    
    private PluginBuild fetchOrThrow(UUID userId, String projectId, UUID buildId) {
        PluginBuild build = buildRepository.findById(buildId)
                .orElseThrow(() -> new NotFoundException("Build not found"));
        
        if (!build.getProjectId().equals(projectId))
            throw new InvalidRequestException("Invalid project id");
        
        if (!build.getOwnerId().equals(userId))
            throw new OwnershipException("User trying to access not his project");
        
        return build;
    }
    
    @Transactional
    public void markRunning(UUID buildId) {
        PluginBuild build = buildRepository.findById(buildId)
                .orElseThrow(() -> new NotFoundException("Build not found"));
        build.setStatus(BuildStatus.RUNNING);
        buildRepository.save(build);
    }
    
    @Transactional
    public void markSuccess(UUID buildId, String artifactKey) {
        PluginBuild build = buildRepository.findById(buildId)
                .orElseThrow(() -> new NotFoundException("Build not found"));
        build.setStatus(BuildStatus.SUCCESS);
        build.setArtifactKey(artifactKey);
        buildRepository.save(build);
    }
    
    @Transactional
    public void markFailed(UUID buildId, String errorMessage) {
        PluginBuild build = buildRepository.findById(buildId)
                .orElseThrow(() -> new NotFoundException("Build not found"));
        build.setStatus(BuildStatus.FAILED);
        build.setErrorMessage(errorMessage);
        buildRepository.save(build);
    }
    
    public ArtifactResponse getArtifact(User user, String projectId, UUID buildId) {
        PluginBuild build = buildRepository.findById(buildId)
                .orElseThrow(() -> new NotFoundException("Build not found"));
        if (!build.getProjectId().equals(projectId))
            throw new InvalidRequestException("Invalid project id");
        if (!build.getOwnerId().equals(user.getId()))
            throw new OwnershipException("User doesn't have access to this build");
        
        return new ArtifactResponse(build.getArtifactKey());
    }
}
