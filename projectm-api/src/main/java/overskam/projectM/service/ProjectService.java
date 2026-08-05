package overskam.projectM.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plummy.visualcore.tools.MapDifferenceExtractor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import overskam.projectM.common.dto.ProjectDeletedMessage;
import overskam.projectM.dto.*;
import overskam.projectM.exception.NotFoundException;
import overskam.projectM.exception.OwnershipException;
import overskam.projectM.model.Project;
import overskam.projectM.repository.mongo.ProjectRepository;
import overskam.projectM.util.SortingUtil;

import java.util.*;
import java.util.function.Consumer;

@Slf4j
@Service
@AllArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final CleanupTaskPublisher cleanupTaskPublisher;
    
    private static final int MAX_PAGE_SIZE = 100;
    
    public Page<ProjectNameResponse> getProjectsList(
            UUID userId, int page, int size, String sortBy, String sortDirection) {
        Sort sort = SortingUtil.forProjects(sortBy, sortDirection);
        log.info("Fetch of projects list for user with id: {} was successful", userId);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(0, page);
        return projectRepository.findProjectNamesByOwnerId(userId, PageRequest.of(safePage, safeSize, sort))
                .map(project -> new ProjectNameResponse(project.getId(), project.getName()));
    }
    
    public ProjectResponse getProject(UUID userId, String projectId) {
        Project project = fetchOrThrow(userId, projectId);
        log.info("Project with id: {} was fetched successfully", projectId);
        return new ProjectResponse(project.getId(), project.getName(), project.getProjectData());
    }
    
    public void deleteProject(UUID userId, String projectId) {
        Project project = fetchOrThrow(userId, projectId);
        projectRepository.delete(project);
        cleanupTaskPublisher.publishProjectDeleted(new ProjectDeletedMessage(projectId, userId));
        log.info("Project with id: {} was deleted successfully", projectId);
    }
    
    public ProjectCreatedResponse createProject(UUID userId, String projectName) {
        Project project = new Project();
        project.setOwnerId(userId);
        project.setName(projectName);
        
        Map<String, Object> defaultProjectData = new LinkedHashMap<>();
        defaultProjectData.put("pluginName", projectName);
        defaultProjectData.put("version", "1.0.0");
        defaultProjectData.put("author", "ProjectM");
        defaultProjectData.put("modules", List.of());
        defaultProjectData.put("variables", new LinkedHashMap<>());
        project.setProjectData(defaultProjectData);
        
        projectRepository.save(project);
        log.info("Project with id: {} was created successfully", project.getId());
        return new ProjectCreatedResponse(project.getId());
    }
    
    public void updateProjectMetadata(UUID userId, String projectId, ProjectMetadataRequest projectMetadataRequest) {
        Project project = fetchOrThrow(userId, projectId);
        setIfNotNull(project::setName, projectMetadataRequest.name());
        projectRepository.save(project);
        log.info("Metadata of project with id: {} was update successfully", projectId);
    }
    
    public void updateProjectData(UUID userId, String projectId, List<SequencedMap<String, Object>> patch) {
        Project project = fetchOrThrow(userId, projectId);
        SequencedMap<String, Object> oldData =
                new LinkedHashMap<>(Optional.ofNullable(project.getProjectData()).orElseGet(LinkedHashMap::new));
        SequencedMap<String, Object> patchedData = MapDifferenceExtractor.restore(oldData, patch);
        project.setProjectData(patchedData);
        projectRepository.save(project);
        log.info("Project data of project with id: {} was updated successfully", projectId);
    }
    
    public void replaceProjectData(UUID userId, String projectId, ReplaceProjectDataRequest changeProjectDataRequest) {
        Project project = fetchOrThrow(userId, projectId);
        project.setProjectData(changeProjectDataRequest.projectData());
        projectRepository.save(project);
        log.info("Data of project with id: {} was update successfully", project.getId());
    }
    
    private Project fetchOrThrow(UUID userId, String projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));
        
        if (!project.getOwnerId().equals(userId))
            throw new OwnershipException("UUID trying to access not his project");
        
        return project;
    }
    
    private <T> void setIfNotNull(Consumer<T> setter, T value) {
        if (value != null)
            setter.accept(value);
    }
}
