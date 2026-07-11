package overskam.projectM.service;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    
    @Value("${app.mail.from}")
    private String from;
    
    @Value("${app.mail.verification-url}")
    private String verifyBaseUrl;
    
    @Value("${app.mail.password-reset-url}")
    private String resetPasswordUrl;
    
    public void sendPasswordResetEmail(String to, String token) {
        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        String link = resetPasswordUrl + "?token=" + encodedToken;
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("Reset your password");
        message.setText("""
                Welcome to ProjectM.

                Reset your password:
                %s

                This link expires in 30 minutes.
                """.formatted(link));
        
        mailSender.send(message);
    }
    
    public void sendVerificationEmail(String to, String token) {
        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        String link = verifyBaseUrl + "?token=" + encodedToken;
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("Verify your ProjectM account");
        message.setText("""
                Welcome to ProjectM.

                Verify your email:
                %s

                This link expires in 30 minutes.
                """.formatted(link));
        
        mailSender.send(message);
    }
}
