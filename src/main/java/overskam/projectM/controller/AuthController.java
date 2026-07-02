package overskam.projectM.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import overskam.projectM.dto.ApiResponse;
import overskam.projectM.dto.LoginRequest;
import overskam.projectM.dto.LoginResponse;
import overskam.projectM.dto.RegisterRequest;
import overskam.projectM.model.CustomUserDetails;
import overskam.projectM.model.User;
import overskam.projectM.service.AuthService;
import overskam.projectM.service.CustomUserDetailsService;
import overskam.projectM.util.JwtUtil;
import overskam.projectM.validation.FullUpdate;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@AllArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final CustomUserDetailsService userDetailsService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
//    private final TokenVersionService tokenVersionService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Validated LoginRequest loginRequest) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.email());
        User user = ((CustomUserDetails) userDetails).getUser();

        log.info("Logging in user...");

        if (!user.getEnabled())
            throw new DisabledException("Please verify your email");

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(userDetails.getUsername(), loginRequest.password())
        );
        String jwtToken = jwtUtil.generateToken(userDetails.getUsername(), user.getId(), user.getTokenVersion());

        return ResponseEntity.ok(new ApiResponse<>("User logged in successfully", new LoginResponse(jwtToken)));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Validated(FullUpdate.class) RegisterRequest registerRequest) {
        log.info("Registering user...");
        authService.register(registerRequest);
        return ResponseEntity.ok(new ApiResponse<>("User has been registered", null));
    }
}
