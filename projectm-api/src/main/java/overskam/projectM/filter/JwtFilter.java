package overskam.projectM.filter;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import overskam.projectM.dto.ApiResponse;
import overskam.projectM.service.CustomUserDetailsService;
import overskam.projectM.service.TokenVersionService;
import overskam.projectM.util.JwtUtil;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@AllArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService customUserDetailsService;
    private final TokenVersionService tokenVersionService;
    private final ObjectMapper objectMapper;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        final String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String jwt = authorizationHeader.substring(7);

            try {
                if (!jwtUtil.isTokenValid(jwt)) {
                    chain.doFilter(request, response);
                    return;
                }

                UUID userId = jwtUtil.extractUserId(jwt);
                Long tokenVersion = jwtUtil.extractVersion(jwt);
                if (userId == null || tokenVersion == null) {
                    writeUnauthorized(response, "Invalid or expired token");
                    return;
                }

                long currentVersion = tokenVersionService.getCurrentVersion(userId);
                if (tokenVersion != currentVersion) {
                    writeUnauthorized(response, "Token revoked");
                    return;
                }

                String username = jwtUtil.extractUsername(jwt);
                if (username != null) {
                    UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (JwtException e) {
                writeUnauthorized(response, "Invalid or expired token");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8);
        objectMapper.writeValue(response.getWriter(), new ApiResponse<>(message, null));
    }
}
