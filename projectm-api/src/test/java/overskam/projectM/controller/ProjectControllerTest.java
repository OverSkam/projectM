package overskam.projectM.controller;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import overskam.projectM.config.SecurityConfig;
import overskam.projectM.dto.ProjectCreatedResponse;
import overskam.projectM.dto.ProjectResponse;
import overskam.projectM.exception.NotFoundException;
import overskam.projectM.filter.JwtFilter;
import overskam.projectM.model.CustomUserDetails;
import overskam.projectM.model.User;
import overskam.projectM.service.ProjectService;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    }
    
    @Nested
    @DisplayName("Create project")
    class CreateProjectTest {
        @Test
        @DisplayName("Creates project and returns its id")
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
        void passesAuthenticatedUserIdToService() throws Exception {
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
        @DisplayName("Answers 400 when the passed data is invalid")
        void returnsBadRequestWhenDataIsInvalid() throws Exception {
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
}
