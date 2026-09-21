package overskam.projectM.controller;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import overskam.projectM.config.SecurityConfig;
import overskam.projectM.dto.*;
import overskam.projectM.exception.NotFoundException;
import overskam.projectM.exception.OwnershipException;
import overskam.projectM.filter.JwtFilter;
import overskam.projectM.model.CustomUserDetails;
import overskam.projectM.model.User;
import overskam.projectM.service.ProjectService;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProjectController.class)
@ActiveProfiles("test")
@Import(SecurityConfig.class)
class ProjectControllerTest {
    @Autowired
    private MockMvc mockMvc;
    
    @MockitoBean
    private ProjectService projectService;
    
    @MockitoBean
    private JwtFilter jwtFilter;
    
    @BeforeEach
    void passThroughJwtFilter() throws Exception {
        doAnswer(invocation -> {
            invocation.getArgument(2, FilterChain.class)
                    .doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtFilter).doFilter(any(), any(), any());
    }
    
    private RequestPostProcessor asUser(UUID userId) {
        User user = new User();
        user.setId(userId);
        user.setEmail("user@test.com");
        user.setPassword("encoded");
        user.setEnabled(true);
        CustomUserDetails principal = new CustomUserDetails(user);
        return authentication(new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()));
    }
    
    @Nested
    @DisplayName("Get project")
    class GetProjectTest {
        @Test
        @DisplayName("Looks the project up for the authenticated user")
        void passesAuthenticatedUserIdToService() throws Exception {
            UUID userId = UUID.randomUUID();
            
            when(projectService.getProject(any(), any()))
                    .thenReturn(new ProjectResponse("abc123", "New project", Map.of()));
            
            mockMvc.perform(get("/api/v1/projects/abc123").with(asUser(userId)))
                    .andExpect(status().isOk());
            
            verify(projectService).getProject(userId, "abc123");
        }
        
        @Test
        @DisplayName("Returns the project in the response envelope")
        void returnsProjectForOwner() throws Exception {
            UUID userId = UUID.randomUUID();
            String projectId = "abc123";
            when(projectService.getProject(userId, projectId))
                    .thenReturn(new ProjectResponse(projectId, "New project", Map.of("version", "1.0.0")));
            
            mockMvc.perform(get("/api/v1/projects/" + projectId).with(asUser(userId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Project was fetched successfully"))
                    .andExpect(jsonPath("$.data.id").value(projectId))
                    .andExpect(jsonPath("$.data.name").value("New project"))
                    .andExpect(jsonPath("$.data.projectData.version").value("1.0.0"));
        }
        
        @Test
        @DisplayName("Answers 404 when the project does not exist")
        void returnsNotFoundWhenProjectDoesNotExist() throws Exception {
            UUID userId = UUID.randomUUID();
            when(projectService.getProject(any(), any()))
                    .thenThrow(new NotFoundException("Project not found"));
            
            mockMvc.perform(get("/api/v1/projects/abc123").with(asUser(userId)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Project not found"));
        }
        
        @Test
        @DisplayName("Rejects a request with no authentication")
        void rejectsAnonymousRequest() throws Exception {
            mockMvc.perform(get("/api/v1/projects/abc123"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Authentication required"));
            verifyNoInteractions(projectService);
        }
        
        @Test
        @DisplayName("Answers 403 when the project belongs to another user")
        void returnsForbiddenWhenUserIsNotOwner() throws Exception {
            UUID userId = UUID.randomUUID();
            when(projectService.getProject(any(), any()))
                    .thenThrow(new OwnershipException("User is trying to access not his project"));
            
            mockMvc.perform(get("/api/v1/projects/abc123").with(asUser(userId)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("User is trying to access not his project"));
        }
    }
    
    @Nested
    @DisplayName("Create project")
    class CreateProjectTest {
        @Test
        @DisplayName("Creates the project and returns its id")
        void returnsCreatedProjectId() throws Exception {
            UUID userId = UUID.randomUUID();
            
            when(projectService.createProject(userId, "My Plugin"))
                    .thenReturn(new ProjectCreatedResponse("abc123"));
            
            mockMvc.perform(post("/api/v1/projects")
                            .with(asUser(userId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "My Plugin"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("New project was created successfully"))
                    .andExpect(jsonPath("$.data.id").value("abc123"));
        }
        
        @Test
        @DisplayName("Creates the project for the authenticated user")
        void passesAuthenticatedUserIdAndNameToService() throws Exception {
            UUID userId = UUID.randomUUID();
            
            when(projectService.createProject(any(), any()))
                    .thenReturn(new ProjectCreatedResponse("abc123"));
            
            mockMvc.perform(post("/api/v1/projects")
                            .with(asUser(userId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "My Plugin"}
                                    """))
                    .andExpect(status().isOk());
            
            verify(projectService).createProject(userId, "My Plugin");
        }
        
        @Test
        @DisplayName("Answers 400 when the name is blank")
        void returnsBadRequestWhenNameIsBlank() throws Exception {
            UUID userId = UUID.randomUUID();
            
            mockMvc.perform(post("/api/v1/projects")
                            .with(asUser(userId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": ""}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.data.name").value("Project name must exist"));
            
            verifyNoInteractions(projectService);
        }
        
        @Test
        @DisplayName("Rejects a request with no authentication")
        void rejectsAnonymousRequest() throws Exception {
            mockMvc.perform(post("/api/v1/projects")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "My plugin"}
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Authentication required"));
            verifyNoInteractions(projectService);
        }
    }
    
    @Nested
    @DisplayName("Get projects")
    class GetProjectsTest {
        @Test
        @DisplayName("Passes the requested paging and sorting through to the service")
        void passesRequestedPagingParameters() throws Exception {
            UUID userId = UUID.randomUUID();
            
            when(projectService.getProjectsList(any(), anyInt(), anyInt(), any(), any()))
                    .thenReturn(Page.empty());
            
            mockMvc.perform(get("/api/v1/projects").with(asUser(userId))
                            .param("page", "1")
                            .param("size", "5")
                            .param("sortBy", "updatedAt")
                            .param("sortDirection", "desc"))
                    .andExpect(status().isOk());
            
            verify(projectService).getProjectsList(userId, 1, 5, "updatedAt", "desc");
        }
        
        @Test
        @DisplayName("Applies default paging and sorting when no query parameters are sent")
        void appliesDefaultPagingParameters() throws Exception {
            UUID userId = UUID.randomUUID();
            
            when(projectService.getProjectsList(any(), anyInt(), anyInt(), any(), any()))
                    .thenReturn(Page.empty());
            
            mockMvc.perform(get("/api/v1/projects").with(asUser(userId)))
                    .andExpect(status().isOk());
            
            verify(projectService).getProjectsList(userId, 0, 10, "name", "asc");
        }
        
        @Test
        @DisplayName("Rejects a request with no authentication")
        void rejectsAnonymousRequest() throws Exception {
            mockMvc.perform(get("/api/v1/projects"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Authentication required"));
            verifyNoInteractions(projectService);
        }
        
        @Test
        @DisplayName("Answers 400 when the page is not a number")
        void rejectsNonNumericPage() throws Exception {
            UUID userId = UUID.randomUUID();
            
            mockMvc.perform(get("/api/v1/projects").with(asUser(userId)).param("page", "abc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid parameter"))
                    .andExpect(jsonPath("$.data.page").value("invalid value"));
            
            verifyNoInteractions(projectService);
        }
        
        @Test
        @DisplayName("Returns the user's projects in the response")
        void returnsProjectsInResponse() throws Exception {
            UUID userId = UUID.randomUUID();
            
            when(projectService.getProjectsList(any(), anyInt(), anyInt(), any(), any()))
                    .thenReturn(new PageImpl<>(List.of(new ProjectNameResponse("abc123", "My Plugin"))));
            
            mockMvc.perform(get("/api/v1/projects").with(asUser(userId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].id").value("abc123"))
                    .andExpect(jsonPath("$.data.content[0].name").value("My Plugin"))
                    .andExpect(jsonPath("$.data.page.totalElements").value(1));
        }
    }
    
    @Nested
    @DisplayName("Delete project")
    class DeleteProjectTest {
        @Test
        @DisplayName("Answers 200 when the project is deleted")
        void returnsOkResponse() throws Exception {
            UUID userId = UUID.randomUUID();
            String projectId = "abc123";
            
            mockMvc.perform(delete("/api/v1/projects/" + projectId).with(asUser(userId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Project was deleted successfully"));
            
            verify(projectService).deleteProject(userId, projectId);
        }
        
        @Test
        @DisplayName("Rejects a request with no authentication")
        void rejectsAnonymousRequest() throws Exception {
            mockMvc.perform(delete("/api/v1/projects/abc123"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Authentication required"));
            verifyNoInteractions(projectService);
        }
        
        @Test
        @DisplayName("Answers 404 when the project does not exist")
        void returnsNotFoundWhenProjectDoesNotExist() throws Exception {
            UUID userId = UUID.randomUUID();
            
            doThrow(new NotFoundException("Project not found")).when(projectService).deleteProject(any(), any());
            
            mockMvc.perform(delete("/api/v1/projects/abc123").with(asUser(userId)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Project not found"));
        }
        
        @Test
        @DisplayName("Answers 403 when the project belongs to another user")
        void returnsForbiddenWhenUserIsNotOwner() throws Exception {
            UUID userId = UUID.randomUUID();
            
            doThrow(new OwnershipException("User is trying to access not his project")).when(projectService).deleteProject(any(), any());
            
            mockMvc.perform(delete("/api/v1/projects/abc123").with(asUser(userId)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("User is trying to access not his project"));
        }
    }
    
    @Nested
    @DisplayName("Update project metadata")
    class UpdateProjectMetadataTest {
        @Test
        @DisplayName("Answers 200 when the project is updated")
        void returnsOkResponse() throws Exception {
            UUID userId = UUID.randomUUID();
            
            mockMvc.perform(patch("/api/v1/projects/abc123/metadata")
                            .with(asUser(userId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "New Name"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message")
                            .value("Projects metadata was updated successfully"))
                    .andExpect(jsonPath("$.data").isEmpty());
            
            verify(projectService).updateProjectMetadata(userId, "abc123", new ProjectMetadataRequest("New Name"));
        }
        
        @Test
        @DisplayName("Answers 400 when the name is empty")
        void returnsBadRequestWhenNameIsEmpty() throws Exception {
            UUID userId = UUID.randomUUID();
            
            mockMvc.perform(patch("/api/v1/projects/abc123/metadata")
                            .with(asUser(userId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": ""}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.data.name").value("Project name must be 1-40 characters"));
            
            verifyNoInteractions(projectService);
        }
        
        @Test
        @DisplayName("Answers 400 when the name is too long")
        void returnsBadRequestWhenNameIsTooLong() throws Exception {
            UUID userId = UUID.randomUUID();
            
            mockMvc.perform(patch("/api/v1/projects/abc123/metadata")
                            .with(asUser(userId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "1234567890|1234567890|1234567890|1234567890|1234567890"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.data.name").value("Project name must be 1-40 characters"));
            
            verifyNoInteractions(projectService);
        }
        
        @Test
        @DisplayName("Accepts a request that leaves the name out")
        void acceptsRequestWithoutName() throws Exception {
            UUID userId = UUID.randomUUID();
            
            mockMvc.perform(patch("/api/v1/projects/abc123/metadata")
                            .with(asUser(userId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Projects metadata was updated successfully"));
            
            verify(projectService)
                    .updateProjectMetadata(userId, "abc123", new ProjectMetadataRequest(null));
        }
        
        @Test
        @DisplayName("Rejects a request with no authentication")
        void rejectsAnonymousRequest() throws Exception {
            mockMvc.perform(patch("/api/v1/projects/abc123/metadata")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "New Name"}
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Authentication required"));
            verifyNoInteractions(projectService);
        }
        
        @Test
        @DisplayName("Answers 404 when the project does not exist")
        void returnsNotFoundWhenProjectDoesNotExist() throws Exception {
            UUID userId = UUID.randomUUID();
            
            doThrow(new NotFoundException("Project not found")).when(projectService).updateProjectMetadata(any(), any(), any());
            
            mockMvc.perform(patch("/api/v1/projects/abc123/metadata")
                            .with(asUser(userId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "New Name"}
                                    """))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Project not found"));
        }
        
        @Test
        @DisplayName("Answers 403 when the project belongs to another user")
        void returnsForbiddenWhenUserIsNotOwner() throws Exception {
            UUID userId = UUID.randomUUID();
            
            doThrow(new OwnershipException("User is trying to access not his project"))
                    .when(projectService).updateProjectMetadata(any(), any(), any());
            
            mockMvc.perform(patch("/api/v1/projects/abc123/metadata")
                            .with(asUser(userId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "New Name"}
                                    """))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("User is trying to access not his project"));
        }
    }
    
    @Nested
    @DisplayName("Update project data")
    class UpdateProjectDataTest {
        @Test
        @DisplayName("Answers 200 when the project is updated")
        void returnsOkResponse() throws Exception {
            UUID userId = UUID.randomUUID();
            
            mockMvc.perform(patch("/api/v1/projects/abc123/data")
                            .with(asUser(userId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    [
                                      { "op": "replace", "path": "/pluginName", "value": "New Name" }
                                    ]
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Projects data was updated successfully"));
            
            verify(projectService).updateProjectData(
                    userId,
                    "abc123",
                    List.of(op("replace", "/pluginName", "New Name"))
            );
        }
        
        @Test
        @DisplayName("Answers 400 when the body is not a list")
        void returnsBadRequestWhenRequestBodyIsNotList() throws Exception {
            UUID userId = UUID.randomUUID();
            
            mockMvc.perform(patch("/api/v1/projects/abc123/data")
                            .with(asUser(userId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Malformed request body"));
            
            verifyNoInteractions(projectService);
        }
    }
    
    @Nested
    @DisplayName("Replace project data")
    class ReplaceProjectDataTest {
        @Test
        @DisplayName("Answers 200 when the project data is replaced")
        void returnsOkResponse() throws Exception {
            UUID userId = UUID.randomUUID();
            
            mockMvc.perform(put("/api/v1/projects/abc123/data")
                            .with(asUser(userId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "projectData": { "pluginName": "New name", "version": "2.0.0" }
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Projects data was updated successfully"));
            
            verify(projectService).replaceProjectData(
                    userId,
                    "abc123",
                    new ReplaceProjectDataRequest(
                            Map.of("pluginName", "New name",
                                    "version", "2.0.0")
                    )
            );
        }
        
        @Test
        @DisplayName("Answers 400 when the project data is left out")
        void returnsBadRequestWhenRequestBodyIsMissing() throws Exception {
            UUID userId = UUID.randomUUID();
            
            mockMvc.perform(put("/api/v1/projects/abc123/data")
                            .with(asUser(userId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.data.projectData").value("Project data is required"));
            
            verifyNoInteractions(projectService);
        }
    }
    
    private static SequencedMap<String, Object> op(String op, String path, Object value) {
        SequencedMap<String, Object> step = new LinkedHashMap<>();
        step.put("op", op);
        step.put("path", path);
        if (value != null) step.put("value", value);
        return step;
    }
}
