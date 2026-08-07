package overskam.projectM.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import overskam.projectM.common.dto.CompileTaskMessage;
import overskam.projectM.common.enums.BuildStatus;
import overskam.projectM.dto.ArtifactResponse;
import overskam.projectM.dto.BuildQueuedResponse;
import overskam.projectM.dto.BuildResponse;
import overskam.projectM.exception.InvalidRequestException;
import overskam.projectM.exception.NotFoundException;
import overskam.projectM.exception.OwnershipException;
import overskam.projectM.model.PluginBuild;
import overskam.projectM.repository.jpa.PluginBuildRepository;
import overskam.projectM.repository.mongo.ProjectRepository;
import overskam.projectM.util.SortingUtil;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class PluginBuildService {
    private final BuildQueuePublisher buildQueuePublisher;
    private final ProjectRepository projectRepository;
    private final PluginBuildRepository buildRepository;
    
    private static final int MAX_PAGE_SIZE = 100;
    
    public Page<BuildResponse> getBuilds(UUID userId, String projectId, int page, int size, String sortBy, String sortDirection) {
        Sort sort = SortingUtil.forBuilds(sortBy, sortDirection);
        log.info("Builds for project with id: {} was fetched successfully", projectId);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(0, page);
        return buildRepository.findByProjectIdAndOwnerId(
                        projectId, userId, PageRequest.of(safePage, safeSize, sort))
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
    
    public BuildResponse getBuild(UUID userId, String projectId, UUID buildId) {
        PluginBuild build = fetchOrThrow(userId, projectId, buildId);
        log.info("Build with id: {} was fetched successfully", buildId);
        
        return new BuildResponse(build.getId(), build.getProjectId(), build.getStatus(), build.getArtifactKey(), build.getErrorMessage());
    }
    
    @Transactional
    public BuildQueuedResponse buildProject(UUID userId, String projectId) {
        if (!projectRepository.existsByIdAndOwnerId(projectId, userId))
            throw new InvalidRequestException("Project doesn't exist or user doesn't have ownership");
        
        if (buildRepository.existsByProjectIdAndStatusIn(projectId, List.of(BuildStatus.QUEUED, BuildStatus.RUNNING)))
            throw new InvalidRequestException("Build already in progress for this project");
        
        PluginBuild build = new PluginBuild();
        build.setProjectId(projectId);
        build.setOwnerId(userId);
        buildRepository.save(build);
        
        CompileTaskMessage message = new CompileTaskMessage(build.getId(), projectId, userId);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                buildQueuePublisher.publish(message);
                log.info("Compile task published, buildId={}", message.buildId());
            }
        });
        
        return new BuildQueuedResponse(build.getId());
    }
    
    public ArtifactResponse getArtifact(UUID userId, String projectId, UUID buildId) {
        PluginBuild build = buildRepository.findById(buildId)
                .orElseThrow(() -> new NotFoundException("Build not found"));
        if (!build.getProjectId().equals(projectId))
            throw new InvalidRequestException("Invalid project id");
        if (!build.getOwnerId().equals(userId))
            throw new OwnershipException("User doesn't have access to this build");
        
        return new ArtifactResponse(build.getArtifactKey());
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
}
