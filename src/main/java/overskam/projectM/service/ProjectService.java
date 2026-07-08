package overskam.projectM.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plummy.visualcore.tools.MapDifferenceExtractor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import overskam.projectM.dto.*;
import overskam.projectM.exception.NotFoundException;
import overskam.projectM.exception.OwnershipException;
import overskam.projectM.model.Project;
import overskam.projectM.model.User;
import overskam.projectM.repository.mongo.ProjectRepository;
import overskam.projectM.util.SortingUtil;

import java.util.*;
import java.util.function.Consumer;

@Slf4j
@Service
@AllArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;
    
    public Page<ProjectNameResponse> getProjectsList(
            User user, int page, int size, String sortBy, String sortDirection) {
        Sort sort = SortingUtil.sortGenerator(sortBy, sortDirection);
        log.info("Fetch of projects list for user with id: {} was successful", user.getId());
        return projectRepository.findByOwnerId(user.getId(), PageRequest.of(page, size, sort))
                .map(project -> new ProjectNameResponse(project.getId(), project.getName()));
    }
    
    public ProjectResponse getProject(User user, String projectId) {
        Project project = fetchOrThrow(user.getId(), projectId);
        log.info("Project with id: {} was fetched successfully", projectId);
        return new ProjectResponse(project.getId(), project.getName(), project.getProjectData());
    }
    
    @Transactional
    public void deleteProject(User user, String projectId) {
        Project project = fetchOrThrow(user.getId(), projectId);
        projectRepository.delete(project);
        log.info("Project with id: {} was deleted successfully", projectId);
    }
    
    @Transactional
    public Map<String, String> createProject(User user, String projectName) {
        Project project = new Project();
        project.setOwnerId(user.getId());
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
        return Map.of("id", project.getId());
    }
    
    @Transactional
    public void updateProjectMetadata(User user, String projectId, ProjectMetadataRequest projectMetadataRequest) {
        Project project = fetchOrThrow(user.getId(), projectId);
        setIfNotNull(project::setName, projectMetadataRequest.name());
        projectRepository.save(project);
        log.info("Metadata of project with id: {} was update successfully", projectId);
    }
    
    @Transactional
    public void updateProjectData(User user, String projectId, List<SequencedMap<String, Object>> patch) {
        Project project = fetchOrThrow(user.getId(), projectId);
        SequencedMap<String, Object> oldData =
                new LinkedHashMap<>(Optional.ofNullable(project.getProjectData()).orElseGet(LinkedHashMap::new));
        SequencedMap<String, Object> patchedData = MapDifferenceExtractor.restore(oldData, patch);
        project.setProjectData(patchedData);
        projectRepository.save(project);
        log.info("Project data of project with id: {} was updated successfully", projectId);
    }
    
    @Transactional
    public void replaceProjectData(User user, String projectId, ReplaceProjectDataRequest changeProjectDataRequest) {
        Project project = fetchOrThrow(user.getId(), projectId);
        project.setProjectData(changeProjectDataRequest.projectData());
        projectRepository.save(project);
        log.info("Data of project with id: {} was update successfully", project);
    }
    
    private Project fetchOrThrow(UUID userId, String projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));
        
        if (!project.getOwnerId().equals(userId))
            throw new OwnershipException("User trying to access not his project");
        
        return project;
    }
    
    private <T> void setIfNotNull(Consumer<T> setter, T value) {
        if (value != null)
            setter.accept(value);
    }
}
