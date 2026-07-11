package overskam.projectM.controller;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import overskam.projectM.config.SecurityConfig;
import overskam.projectM.dto.EmailRequest;
import overskam.projectM.dto.LoginRequest;
import overskam.projectM.dto.PasswordResetRequest;
import overskam.projectM.dto.RegisterRequest;
import overskam.projectM.filter.JwtFilter;
import overskam.projectM.model.CustomUserDetails;
import overskam.projectM.model.User;
import overskam.projectM.service.AuthService;
import overskam.projectM.service.CustomUserDetailsService;
import overskam.projectM.service.TokenVersionService;
import overskam.projectM.util.JwtUtil;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
@Import(SecurityConfig.class)
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private TokenVersionService tokenVersionService;

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

    @Test
    void loginReturnsJwtForEnabledUser() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = user(userId, true, 3L);
        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(new CustomUserDetails(user));
        when(jwtUtil.generateToken("user@test.com", userId, 3L)).thenReturn("jwt-token");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new LoginRequest("user@test.com", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User logged in successfully"))
                .andExpect(jsonPath("$.data.token").value("jwt-token"));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void loginReturnsForbiddenForDisabledUser() throws Exception {
        User user = user(UUID.randomUUID(), false, 0L);
        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(new CustomUserDetails(user));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new LoginRequest("user@test.com", "password123"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Please verify your email"));
    }

    @Test
    void registerBindsJsonValidatesAndDelegates() throws Exception {
        RegisterRequest request = new RegisterRequest("user@test.com", "password123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User has been registered"));

        verify(authService).register(request);
    }

    @Test
    void registerRejectsInvalidRequestBody() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content("""
                                {
                                  "email": "not-email",
                                  "password": "short"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyRequiresTokenParam() throws Exception {
        mockMvc.perform(get("/api/v1/auth/verify"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyDelegatesTokenParam() throws Exception {
        mockMvc.perform(get("/api/v1/auth/verify").param("token", "abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User has been verified"));

        verify(authService).verify("abc");
    }

    @Test
    void resendVerificationReturnsGenericMessage() throws Exception {
        EmailRequest request = new EmailRequest("user@test.com");

        mockMvc.perform(post("/api/v1/auth/resend-verification")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("If the email exists and is unverified, a verification link has been sent"));

        verify(authService).resendVerification(request);
    }

    @Test
    void forgotPasswordReturnsGenericMessage() throws Exception {
        EmailRequest request = new EmailRequest("user@test.com");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("If an account exists for this email, a password reset link has been sent"));

        verify(authService).requestPasswordReset(request);
    }

    @Test
    void resetPasswordDelegates() throws Exception {
        PasswordResetRequest request = new PasswordResetRequest("password123", "token");

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully"));

        verify(authService).resetPassword(request);
    }

    @Test
    void resetPasswordRejectsInvalidRequestBody() throws Exception {
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType("application/json")
                        .content("""
                                {
                                  "password": "short",
                                  "token": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void serviceExceptionUsesGlobalExceptionHandler() throws Exception {
        doThrow(new DisabledException("Please verify your email"))
                .when(authService).verify("disabled");

        mockMvc.perform(get("/api/v1/auth/verify").param("token", "disabled"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Please verify your email"));
    }

    @Test
    void logoutAllUsesAuthenticationPrincipalWhenPresent() throws Exception {
        UUID userId = UUID.randomUUID();
        CustomUserDetails principal = new CustomUserDetails(user(userId, true, 1L));
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        mockMvc.perform(post("/api/v1/auth/logout-all").with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("All sessions revoked"));

        verify(tokenVersionService).bumpVersion(userId);
    }

    @Test
    void logoutAllRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout-all"))
                .andExpect(status().isForbidden());
    }

    private static User user(UUID id, boolean enabled, long tokenVersion) {
        User user = new User();
        user.setId(id);
        user.setEmail("user@test.com");
        user.setPassword("encoded");
        user.setEnabled(enabled);
        user.setTokenVersion(tokenVersion);
        return user;
    }
}
