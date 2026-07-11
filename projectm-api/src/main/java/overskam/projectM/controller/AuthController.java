package overskam.projectM.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import overskam.projectM.dto.*;
import overskam.projectM.model.CustomUserDetails;
import overskam.projectM.model.User;
import overskam.projectM.service.AuthService;
import overskam.projectM.service.CustomUserDetailsService;
import overskam.projectM.service.TokenVersionService;
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
    private final TokenVersionService tokenVersionService;
    
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
    
    @GetMapping("/verify")
    public ResponseEntity<?> verify(@RequestParam String token) {
        log.info("Trying to verify user's email...");
        authService.verify(token);
        
        return ResponseEntity.ok(new ApiResponse<>("User has been verified", null));
    }
    
    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestBody EmailRequest emailRequest) {
        log.info("User is trying to get new verification email");
        authService.resendVerification(emailRequest);
        
        return ResponseEntity.ok(
                new ApiResponse<>(
                        "If the email exists and is unverified, a verification link has been sent",
                        null)
        );
    }
    
    @PostMapping("/forgot-password")
    public ResponseEntity<?> requestPasswordReset(@RequestBody EmailRequest emailRequest) {
        log.info("User is requesting password reset for account with email: {}", emailRequest.email());
        authService.requestPasswordReset(emailRequest);
        
        return ResponseEntity.ok(
                new ApiResponse<>(
                        "If an account exists for this email, a password reset link has been sent",
                        null)
        );
    }
    
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody @Validated PasswordResetRequest passwordResetRequest) {
        log.info("User is trying to verify password reset");
        authService.resetPassword(passwordResetRequest);
        
        return ResponseEntity.ok(new ApiResponse<>("Password changed successfully", null));
    }
    
    @PostMapping("/logout-all")
    public ResponseEntity<?> logoutAll(@AuthenticationPrincipal CustomUserDetails principal) {
        tokenVersionService.bumpVersion(principal.getUser().getId());
        return ResponseEntity.ok(new ApiResponse<>("All sessions revoked", null));
    }
}
