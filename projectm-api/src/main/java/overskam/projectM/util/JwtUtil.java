package overskam.projectM.util;

import io.github.cdimascio.dotenv.Dotenv;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {
    private static final long TOKEN_TTL_MS = 1000L * 60 * 60 * 24 * 7;

    private final SecretKey secretKey;
    
    public JwtUtil(@Value("${app.jwt.secret}") String secret) {
        byte[] keyBytes = Base64.getUrlDecoder().decode(secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }
        
        public String generateToken(String username, UUID userId, long version) {
        return Jwts.builder()
                .subject(username)
                .claim("uid", userId)
                .claim("ver", version)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + TOKEN_TTL_MS))
                .signWith(secretKey)
                .compact();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public UUID extractUserId(String token) {
        Object uid = extractAllClaims(token).get("uid");
        return uid == null ? null : UUID.fromString(uid.toString());
    }

    public Long extractVersion(String token) {
        Object ver = extractAllClaims(token).get("ver");
        return ver == null ? null : ((Number) ver).longValue();
    }

    public boolean isTokenValid(String token) {
        return !isTokenExpired(token);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }
}