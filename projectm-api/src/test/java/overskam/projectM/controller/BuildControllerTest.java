package overskam.projectM.controller;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import overskam.projectM.config.SecurityConfig;
import overskam.projectM.filter.JwtFilter;
import overskam.projectM.model.CustomUserDetails;
import overskam.projectM.model.User;
import overskam.projectM.service.PluginBuildService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

@WebMvcTest(BuildController.class)
@ActiveProfiles("test")
@Import(SecurityConfig.class)
public class BuildControllerTest {
    
    private static final UUID USER_ID = UUID.randomUUID();
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockitoBean
    private PluginBuildService pluginBuildService;
    
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
    
    
}
