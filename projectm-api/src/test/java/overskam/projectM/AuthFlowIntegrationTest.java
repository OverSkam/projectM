package overskam.projectM;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import overskam.projectM.model.User;
import overskam.projectM.repository.jpa.UserRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.jwt.secret=dGVzdC1zZWNyZXQtZm9yLXVuaXQtdGVzdHMtb25seS0zMi1ieXRlcw")
@AutoConfigureMockMvc
class AuthFlowIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Test
    @DisplayName("Logs in and uses the token to reach a protected endpoint")
    void logsInAndReachesProtectedEndpoint() throws Exception {
        User user = new User();
        user.setPassword(passwordEncoder.encode("longPassword"));
        user.setEmail("test@test.com");
        user.setEnabled(true);
        userRepository.save(user);
        
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                "email": "test@test.com",
                                "password": "longPassword"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        String token = JsonPath.read(body, "$.data.token");
        
        mockMvc.perform(get("/api/v1/projects").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
