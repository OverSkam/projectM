package overskam.projectM.filter;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import overskam.projectM.model.CustomUserDetails;
import overskam.projectM.model.User;
import overskam.projectM.service.CustomUserDetailsService;
import overskam.projectM.service.TokenVersionService;
import overskam.projectM.util.JwtUtil;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JwtFilterTest {
    private final JwtUtil jwtUtil = mock(JwtUtil.class);
    private final CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
    private final TokenVersionService tokenVersionService = mock(TokenVersionService.class);
    private final JwtFilter filter = new JwtFilter(jwtUtil, userDetailsService, tokenVersionService);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requestWithoutBearerTokenContinuesUnauthenticated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtUtil, userDetailsService, tokenVersionService);
    }

    @Test
    void validTokenAuthenticatesUserAndContinues() throws Exception {
        UUID userId = UUID.randomUUID();
        MockHttpServletRequest request = requestWithBearer("valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        User user = new User();
        user.setEmail("user@test.com");
        user.setPassword("encoded");
        user.setEnabled(true);

        when(jwtUtil.isTokenValid("valid-token")).thenReturn(true);
        when(jwtUtil.extractUserId("valid-token")).thenReturn(userId);
        when(jwtUtil.extractVersion("valid-token")).thenReturn(3L);
        when(tokenVersionService.getCurrentVersion(userId)).thenReturn(3L);
        when(jwtUtil.extractUsername("valid-token")).thenReturn("user@test.com");
        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(new CustomUserDetails(user));

        filter.doFilterInternal(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("user@test.com");
    }

    @Test
    void revokedTokenReturnsUnauthorized() throws Exception {
        UUID userId = UUID.randomUUID();
        MockHttpServletRequest request = requestWithBearer("revoked-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtil.isTokenValid("revoked-token")).thenReturn(true);
        when(jwtUtil.extractUserId("revoked-token")).thenReturn(userId);
        when(jwtUtil.extractVersion("revoked-token")).thenReturn(1L);
        when(tokenVersionService.getCurrentVersion(userId)).thenReturn(2L);

        filter.doFilterInternal(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Token revoked");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void malformedTokenReturnsUnauthorized() throws Exception {
        MockHttpServletRequest request = requestWithBearer("bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtil.isTokenValid("bad-token")).thenThrow(new JwtException("bad"));

        filter.doFilterInternal(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Invalid or expired token");
    }

    private static MockHttpServletRequest requestWithBearer(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }
}
