//package overskam.projectM.util;
//
//import io.github.cdimascio.dotenv.Dotenv;
//import io.jsonwebtoken.JwtException;
//import org.junit.jupiter.api.Test;
//
//import java.nio.charset.StandardCharsets;
//import java.util.Base64;
//import java.util.UUID;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.when;
//
//class JwtUtilTest {
//
//    @Test
//    void generateTokenCanBeParsedBackToExpectedClaims() {
//        JwtUtil jwtUtil = jwtUtil();
//        UUID userId = UUID.randomUUID();
//
//        String token = jwtUtil.generateToken("user@test.com", userId, 5L);
//
//        assertThat(jwtUtil.extractUsername(token)).isEqualTo("user@test.com");
//        assertThat(jwtUtil.extractUserId(token)).isEqualTo(userId);
//        assertThat(jwtUtil.extractVersion(token)).isEqualTo(5L);
//        assertThat(jwtUtil.isTokenValid(token)).isTrue();
//    }
//
//    @Test
//    void invalidTokenThrowsJwtException() {
//        JwtUtil jwtUtil = jwtUtil();
//
//        assertThatThrownBy(() -> jwtUtil.extractUsername("not-a-jwt"))
//                .isInstanceOf(JwtException.class);
//    }
//
////    private static JwtUtil jwtUtil() {
////        Dotenv dotenv = mock(Dotenv.class);
////        String secret = Base64.getUrlEncoder()
////                .withoutPadding()
////                .encodeToString("12345678901234567890123456789012".getBytes(StandardCharsets.UTF_8));
////        when(dotenv.get("JWT_TOKEN_SECRET")).thenReturn(secret);
////        return new JwtUtil(dotenv);
////    }
//}
