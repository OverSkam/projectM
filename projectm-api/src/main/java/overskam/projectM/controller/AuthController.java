package overskam.projectM.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import overskam.projectM.dto.*;
import overskam.projectM.model.CustomUserDetails;
import overskam.projectM.service.AuthService;
import overskam.projectM.service.TokenVersionService;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@AllArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final TokenVersionService tokenVersionService;
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest loginRequest) {
        log.info("Logging in user...");
        
        return ResponseEntity.ok(
                new ApiResponse<>(
                        "User logged in successfully",
                        new LoginResponse(authService.login(loginRequest))
                )
        );
    }
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequest registerRequest) {
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
    public ResponseEntity<?> resendVerification(@RequestBody @Valid EmailRequest emailRequest) {
        log.info("User is trying to get new verification email");
        authService.resendVerification(emailRequest);
        
        return ResponseEntity.ok(
                new ApiResponse<>(
                        "If the email exists and is unverified, a verification link has been sent",
                        null)
        );
    }
    
    @PostMapping("/forgot-password")
    public ResponseEntity<?> requestPasswordReset(@RequestBody @Valid EmailRequest emailRequest) {
        log.info("User is requesting password reset for account with email: {}", emailRequest.email());
        authService.requestPasswordReset(emailRequest);
        
        return ResponseEntity.ok(
                new ApiResponse<>(
                        "If an account exists for this email, a password reset link has been sent",
                        null)
        );
    }
    
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody @Valid PasswordResetRequest passwordResetRequest) {
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
