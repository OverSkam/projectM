package overskam.projectM.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.data.domain.*;
import overskam.projectM.common.dto.ProjectDeletedMessage;
import overskam.projectM.dto.*;
import overskam.projectM.exception.InvalidRequestException;
import overskam.projectM.exception.NotFoundException;
import overskam.projectM.exception.OwnershipException;
import overskam.projectM.model.Project;
import overskam.projectM.repository.mongo.ProjectRepository;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {
    
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private CleanupTaskPublisher cleanupTaskPublisher;
    
    @InjectMocks
    private ProjectService projectService;
    
    @Nested
    @DisplayName("Get project tests")
    class GetProjectTests {
        
        @Test
        @DisplayName("Returns project successfully when id is correct and project is accessible")
        void returnsProjectWhenIdIsCorrectAndUserIsOwner() {
            UUID userId = UUID.randomUUID();
            Project project = new Project();
            project.setId("abc123");
            project.setOwnerId(userId);
            project.setName("Project name");
            project.setProjectData(Map.of("version", "1.0.0"));
            
            when(projectRepository.findById("abc123")).thenReturn(Optional.of(project));
            
            ProjectResponse response = projectService.getProject(userId, "abc123");
            
            assertEquals("abc123", response.id());
            assertEquals("Project name", response.name());
            assertEquals(Map.of("version", "1.0.0"), response.projectData());
        }
        
        @Test
        @DisplayName("Throws NotFoundException when id is not correct")
        void throwsNotFoundExceptionWhenProjectDoesntExist() {
            UUID userId = UUID.randomUUID();
            when(projectRepository.findById("abc123")).thenReturn(Optional.empty());
            
            assertThrowsExactly(NotFoundException.class, () -> projectService.getProject(userId, "abc123"));
        }
        
        @Test
        @DisplayName("Throws OwnershipException when id is correct and user id is not matching owner id")
        void throwsOwnershipExceptionWhenUserIsNotOwner() {
            UUID userId = UUID.randomUUID();
            UUID otherOwnerId = UUID.randomUUID();
            Project project = new Project();
            project.setId("abc123");
            project.setOwnerId(otherOwnerId);
            
            when(projectRepository.findById("abc123")).thenReturn(Optional.of(project));
            
            assertThrowsExactly(OwnershipException.class, () -> projectService.getProject(userId, "abc123"));
        }
    }
    
    @Nested
    @DisplayName("Get projects list")
    class GetProjectsListTest {
        @Test
        @DisplayName("Returns a page of id and name pairs for the caller's projects")
        void returnsPageOfProjectsOwnedByUser() {
            UUID userId = UUID.randomUUID();
            PageRequest expectedPageable = PageRequest.of(0, 10, Sort.by("name").ascending());
            Page<ProjectRepository.ProjectNameView> page = new PageImpl<>(
                    List.of(view("abc123", "First"), view("def456", "Second")),
                    expectedPageable,
                    2);
            
            when(projectRepository.findProjectNamesByOwnerId(userId, expectedPageable)).thenReturn(page);
            
            Page<ProjectNameResponse> result =
                    projectService.getProjectsList(userId, 0, 10, "name", "asc");
            
            assertEquals(2, result.getTotalElements());
            assertEquals("abc123", result.getContent().getFirst().id());
            assertEquals("First", result.getContent().getFirst().name());
            assertEquals("Second", result.getContent().get(1).name());
        }
        
        @Test
        @DisplayName("Returns an empty page for user with no projects")
        void returnsAnEmptyPageOfProjectsOwnedByUser() {
            UUID userId = UUID.randomUUID();
            PageRequest expectedPageable = PageRequest.of(0, 10, Sort.by("name").ascending());
            Page<ProjectRepository.ProjectNameView> page = new PageImpl<>(List.of(), expectedPageable, 0);
            
            when(projectRepository.findProjectNamesByOwnerId(userId, expectedPageable)).thenReturn(page);
            
            Page<ProjectNameResponse> result =
                    projectService.getProjectsList(userId, 0, 10, "name", "asc");
            
            assertEquals(0, result.getTotalElements());
        }
        
        @Test
        @DisplayName("Throws InvalidRequestException when sortBy is invalid")
        void throwsInvalidRequestExceptionWhenSortByIsInvalid() {
            UUID userId = UUID.randomUUID();
            
            assertThrowsExactly(InvalidRequestException.class,
                    () -> projectService.getProjectsList(userId, 0, 10, "not name", "asc"));
            verifyNoInteractions(projectRepository);
        }
        
        @ParameterizedTest(name = "page {0} and size {1} become page {2} and size {3}")
        @CsvSource({
                "0,  5000, 0, 100",
                "0,  0,    0, 1",
                "-1, 10,   0, 10"
        })
        @DisplayName("Clamps page and size to a safe range")
        void clampsPagingArguments(int page, int size, int expectedPage, int expectedSize) {
            UUID userId = UUID.randomUUID();
            when(projectRepository.findProjectNamesByOwnerId(eq(userId), any(Pageable.class)))
                    .thenReturn(Page.empty());
            
            projectService.getProjectsList(userId, page, size, "name", "asc");
            
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(projectRepository).findProjectNamesByOwnerId(eq(userId), captor.capture());
            
            assertEquals(expectedPage, captor.getValue().getPageNumber());
            assertEquals(expectedSize, captor.getValue().getPageSize());
        }
        
        @Test
        @DisplayName("Translate sortBy to acceptable")
        void translateSortByToAcceptable() {
            UUID userId = UUID.randomUUID();
            when(projectRepository.findProjectNamesByOwnerId(eq(userId), any(Pageable.class)))
                    .thenReturn(Page.empty());
            
            projectService.getProjectsList(userId, 0, 10, "updatedAt", "desc");
            
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(projectRepository).findProjectNamesByOwnerId(eq(userId), captor.capture());
            
            Sort.Order order = captor.getValue().getSort().getOrderFor("lastModifiedAt");
            assertNotNull(order);
            assertTrue(order.isDescending());
            assertNull(captor.getValue().getSort().getOrderFor("updatedAt"));
        }
        
        @Test
        @DisplayName("Translate sort direction to acceptable")
        void translateSortDirectionToAcceptable() {
            UUID userId = UUID.randomUUID();
            when(projectRepository.findProjectNamesByOwnerId(eq(userId), any(Pageable.class)))
                    .thenReturn(Page.empty());
            
            projectService.getProjectsList(userId, 0, 10, "updatedAt", "banana");
            
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(projectRepository).findProjectNamesByOwnerId(eq(userId), captor.capture());
            
            Sort.Order order = captor.getValue().getSort().getOrderFor("lastModifiedAt");
            assertNotNull(order);
            assertTrue(order.isAscending());
        }
    }
    
    @Nested
    @DisplayName("Delete project")
    class DeleteProjectTest {
        private static final String PROJECT_ID = "abc123";
        
        @Test
        @DisplayName("Deletes the project and schedules cleanup of its builds")
        void successfullyDeleteProject() {
            UUID userId = UUID.randomUUID();
            Project project = new Project();
            project.setId(PROJECT_ID);
            project.setOwnerId(userId);
            
            
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            projectService.deleteProject(userId, PROJECT_ID);
            
            verify(projectRepository).delete(project);
            verify(cleanupTaskPublisher).publishProjectDeleted(new ProjectDeletedMessage(PROJECT_ID, userId));
        }
        
        @Test
        @DisplayName("Reports not found when no project has that id")
        void throwsNotFoundExceptionWhenProjectDoesntExists() {
            UUID userId = UUID.randomUUID();
            
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());
            
            assertThrowsExactly(NotFoundException.class, () -> projectService.deleteProject(userId, PROJECT_ID));
            verify(projectRepository, never()).delete(any(Project.class));
            verifyNoInteractions(cleanupTaskPublisher);
        }
        
        @Test
        @DisplayName("Deletes nothing when the project belongs to another user")
        void throwsOwnershipExceptionWhenUserIsNotOwner() {
            UUID userId = UUID.randomUUID();
            UUID otherOwnerId = UUID.randomUUID();
            Project project = new Project();
            project.setId(PROJECT_ID);
            project.setOwnerId(otherOwnerId);
            
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            
            assertThrowsExactly(OwnershipException.class, () -> projectService.deleteProject(userId, PROJECT_ID));
            verify(projectRepository, never()).delete(any(Project.class));
            verifyNoInteractions(cleanupTaskPublisher);
        }
        
        @Test
        @DisplayName("Leaves the project deleted when the cleanup message cannot be published")
        void propagatesFailureAfterProjectIsAlreadyDeleted() {
            UUID userId = UUID.randomUUID();
            Project project = new Project();
            project.setId(PROJECT_ID);
            project.setOwnerId(userId);
            
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            doThrow(new AmqpException("broker down"))
                    .when(cleanupTaskPublisher).publishProjectDeleted(any(ProjectDeletedMessage.class));
            
            assertThrowsExactly(AmqpException.class,
                    () -> projectService.deleteProject(userId, PROJECT_ID));
            
            verify(projectRepository).delete(project);
        }
    }
    
    @Nested
    @DisplayName("Create project")
    class CreateProjectTest {
        
        @Test
        @DisplayName("Saves a project owned by the caller with the default plugin document")
        void savesProjectWithDefaultDocument() {
            UUID userId = UUID.randomUUID();
            String projectName = "Project Name";
            
            projectService.createProject(userId, projectName);
            
            ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
            verify(projectRepository).save(captor.capture());
            
            Project saved = captor.getValue();
            assertEquals(userId, saved.getOwnerId());
            assertEquals(projectName, saved.getName());
            assertEquals(
                    Map.of("pluginName", projectName,
                            "version", "1.0.0",
                            "author", "ProjectM",
                            "modules", List.of(),
                            "variables", Map.of()),
                    saved.getProjectData());
        }
        
        @Test
        @DisplayName("Returns the id the database assigned to the new project")
        void returnsGeneratedId() {
            UUID userId = UUID.randomUUID();
            
            when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
                Project toSave = invocation.getArgument(0);
                toSave.setId("generated-id");
                return toSave;
            });
            
            ProjectCreatedResponse response = projectService.createProject(userId, "Project Name");
            
            assertEquals("generated-id", response.id());
        }
    }
    
    @Nested
    @DisplayName("Projects metadata update")
    class ProjectsMetadataUpdateTest {
        @Test
        @DisplayName("Updates projects metadata when project name is not null")
        void updatesProjectsMetadata() {
            UUID userId = UUID.randomUUID();
            String projectName = "First name";
            String newName = "Second name";
            String projectId = "abc123";
            
            Project project = new Project();
            project.setName(projectName);
            project.setOwnerId(userId);
            project.setId(projectId);
            
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
            projectService.updateProjectMetadata(userId, projectId, new ProjectMetadataRequest(newName));
            
            ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
            verify(projectRepository).save(captor.capture());
            
            Project result = captor.getValue();
            assertEquals(newName, result.getName());
        }
        
        @Test
        @DisplayName("Keeps the current name when the request omits it")
        void keepsExistingNameWhenNewNameIsNull() {
            UUID userId = UUID.randomUUID();
            String originalName = "First name";
            String projectId = "abc123";
            Project project = new Project();
            project.setName(originalName);
            project.setOwnerId(userId);
            project.setId(projectId);
            
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
            projectService.updateProjectMetadata(userId, projectId, new ProjectMetadataRequest(null));
            
            ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
            verify(projectRepository).save(captor.capture());
            
            Project result = captor.getValue();
            assertEquals(originalName, result.getName());
        }
        
        @Test
        @DisplayName("Changes nothing when the project belongs to another user")
        void doesNotUpdateWhenUserIsNotOwner() {
            projectOwnedBySomeoneElse("abc123");
            
            assertThrowsExactly(OwnershipException.class,
                    () -> projectService.updateProjectMetadata(UUID.randomUUID(), "abc123", new ProjectMetadataRequest("New name")));
            
            verify(projectRepository, never()).save(any(Project.class));
        }
    }
    
    @Nested
    @DisplayName("Projects data replacement")
    class ProjectsDataReplacement {
        @Test
        @DisplayName("Replaces projects data entirely")
        void replacesProjectsData() {
            UUID userId = UUID.randomUUID();
            String projectId = "abc123";
            var oldData = Map.of("pluginName", "Old name",
                    "version", "1.0.0",
                    "author", "ProjectM",
                    "modules", List.of(),
                    "variables", Map.of());
            var newData = Map.of("pluginName", "New name",
                    "version", "2.0.0",
                    "author", "ProjectX",
                    "modules", List.of(),
                    "variables", Map.of());
            Project project = new Project();
            project.setId(projectId);
            project.setOwnerId(userId);
            project.setProjectData(oldData);
            
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
            projectService.replaceProjectData(userId, projectId, new ReplaceProjectDataRequest(newData));
            
            ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
            verify(projectRepository).save(captor.capture());
            
            Project result = captor.getValue();
            assertEquals(newData, result.getProjectData());
        }
        
        @Test
        @DisplayName("Changes nothing when the project belongs to another user")
        void doesNotReplaceWhenUserIsNotOwner() {
            projectOwnedBySomeoneElse("abc123");
            
            assertThrowsExactly(OwnershipException.class,
                    () -> projectService.replaceProjectData(UUID.randomUUID(), "abc123",
                            new ReplaceProjectDataRequest(
                                    Map.of("pluginName", "New name",
                                            "version", "2.0.0",
                                            "author", "ProjectM",
                                            "modules", List.of(),
                                            "variables", Map.of())
                            )
                    )
            );
            
            verify(projectRepository, never()).save(any(Project.class));
        }
    }
    
    private static ProjectRepository.ProjectNameView view(String id, String name) {
        return new ProjectRepository.ProjectNameView() {
            @Override
            public String getId() {
                return id;
            }
            
            @Override
            public String getName() {
                return name;
            }
        };
    }
    
    private Project projectOwnedBySomeoneElse(String projectId) {
        Project project = new Project();
        project.setId(projectId);
        project.setOwnerId(UUID.randomUUID());
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        return project;
    }
}