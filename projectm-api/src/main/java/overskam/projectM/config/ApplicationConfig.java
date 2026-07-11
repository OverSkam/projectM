package overskam.projectM.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {
    @Bean
    public Dotenv dotenv() {
        return Dotenv.configure()
                .directory("./") // Look in the root directory
                .ignoreIfMissing() // CRITICAL: Don't crash if the file isn't there
                .load();
    }

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();

        mapper.getConfiguration()
                .setFieldMatchingEnabled(true)
                .setSkipNullEnabled(true);

        return mapper;
    }
}
