package overskam.projectM;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import overskam.projectM.repository.jpa.PluginBuildRepository;
import overskam.projectM.repository.jpa.UserRepository;
import overskam.projectM.repository.jpa.VerificationTokenRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(
		properties = {
				"spring.autoconfigure.exclude="
						+ "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
						+ "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
						+ "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration,"
						+ "org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration,"
						+ "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration,"
						+ "org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration"
		}
)
class ProjectMApplicationTests {
	@Autowired
	private ApplicationContext applicationContext;

	@MockitoBean
	private UserRepository userRepository;
    
    @MockitoBean
    private PluginBuildRepository pluginBuildRepository;

	@MockitoBean
	private VerificationTokenRepository verificationTokenRepository;

	@MockitoBean
	private JavaMailSender javaMailSender;

	@MockitoBean
	private StringRedisTemplate stringRedisTemplate;

	@Test
	void contextLoadsWithTestSafeInfrastructure() {
		assertThat(applicationContext).isNotNull();
		assertThat(applicationContext.getBean(ProjectMApplication.class)).isNotNull();
	}

}
