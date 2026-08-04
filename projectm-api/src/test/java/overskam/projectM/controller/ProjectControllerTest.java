package overskam.projectM.controller;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import overskam.projectM.config.SecurityConfig;
import overskam.projectM.dto.ProjectNameResponse;
import overskam.projectM.dto.ProjectResponse;
import overskam.projectM.exception.NotFoundException;
import overskam.projectM.exception.OwnershipException;
import overskam.projectM.filter.JwtFilter;
import overskam.projectM.model.CustomUserDetails;
import overskam.projectM.model.User;
import overskam.projectM.service.ProjectService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProjectController.class)
@ActiveProfiles("test")
@Import(SecurityConfig.class)
class ProjectControllerTest {
    
    private static final UUID USER_ID = UUID.randomUUID();
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockitoBean
    private ProjectService projectService;
    
    @MockitoBean
    private JwtFilter jwtFilter;
    
    @BeforeEach
    void passThroughJwtFilter() throws Exception {
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
    }
    
    private static RequestPostProcessor asUser() {
        User user = new User();
        user.setId(USER_ID);
        user.setEmail("user@test.com");
        user.setPassword("encoded");
        user.setEnabled(true);
        user.setTokenVersion(0L);
        CustomUserDetails principal = new CustomUserDetails(user);
        return authentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
    
    @Test
    void projectsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void getProjectsListReturnsNames() throws Exception {
        Page<ProjectNameResponse> page =
                new PageImpl<>(List.of(new ProjectNameResponse("abc123", "My Plugin")));
        when(projectService.getProjectsList(eq(USER_ID), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(page);
        
        mockMvc.perform(get("/api/v1/projects").with(asUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Projects list was fetched successfully"))
                .andExpect(jsonPath("$.data.content[0].name").value("My Plugin"));
    }
    
    @Test
    void getProjectReturnsNotFound() throws Exception {
        when(projectService.getProject(USER_ID, "missing"))
                .thenThrow(new NotFoundException("Project not found"));
        
        mockMvc.perform(get("/api/v1/projects/missing").with(asUser()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Project not found"));
    }
    
    @Test
    void getForeignProjectReturnsForbidden() throws Exception {
        when(projectService.getProject(USER_ID, "foreign"))
                .thenThrow(new OwnershipException("not your project"));
        
        mockMvc.perform(get("/api/v1/projects/foreign").with(asUser()))
                .andExpect(status().isForbidden());
    }
    
    @Test
    void getProjectReturnsData() throws Exception {
        when(projectService.getProject(USER_ID, "abc123"))
                .thenReturn(new ProjectResponse("abc123", "My Plugin", Map.of("version", "1.0.0")));
        
        mockMvc.perform(get("/api/v1/projects/abc123").with(asUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("My Plugin"))
                .andExpect(jsonPath("$.data.projectData.version").value("1.0.0"));
    }
    
    @Test
    void createProjectDelegatesToService() throws Exception {
        when(projectService.createProject(USER_ID, "My Plugin")).thenReturn(Map.of("id", "abc123"));
        
        mockMvc.perform(post("/api/v1/projects").with(asUser())
                        .contentType("application/json")
                        .content("""
                                {"name": "My Plugin"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("abc123"));
        
        verify(projectService).createProject(USER_ID, "My Plugin");
    }
    
    @Test
    void createProjectRejectsBlankName() throws Exception {
        mockMvc.perform(post("/api/v1/projects").with(asUser())
                        .contentType("application/json")
                        .content("""
                                {"name": "   "}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.data.name").exists());
        
        verifyNoInteractions(projectService);
    }
    
    @Test
    void createProjectRejectsTooLongName() throws Exception {
        mockMvc.perform(post("/api/v1/projects").with(asUser())
                        .contentType("application/json")
                        .content("{\"name\": \"" + "x".repeat(41) + "\"}"))
                .andExpect(status().isBadRequest());
        
        verifyNoInteractions(projectService);
    }
    
    @Test
    void updateMetadataAllowsMissingName() throws Exception {
        mockMvc.perform(patch("/api/v1/projects/abc123/metadata").with(asUser())
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk());
        
        verify(projectService).updateProjectMetadata(eq(USER_ID), eq("abc123"), any());
    }
    
    @Test
    void updateMetadataRejectsBlankName() throws Exception {
        mockMvc.perform(patch("/api/v1/projects/abc123/metadata").with(asUser())
                        .contentType("application/json")
                        .content("""
                                {"name": ""}"""))
                .andExpect(status().isBadRequest());
        
        verifyNoInteractions(projectService);
    }
    
    @Test
    void replaceDataRejectsMissingProjectData() throws Exception {
        mockMvc.perform(put("/api/v1/projects/abc123/data").with(asUser())
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.projectData").exists());
        
        verifyNoInteractions(projectService);
    }
    
    @Test
    void deleteProjectDelegatesToService() throws Exception {
        mockMvc.perform(delete("/api/v1/projects/abc123").with(asUser()))
                .andExpect(status().isOk());
        
        verify(projectService).deleteProject(USER_ID, "abc123");
    }
}