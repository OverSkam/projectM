package overskam.projectM.worker.config;

import org.plummy.visualcore.tools.PluginCompiler;
import org.plummy.visualcore.tools.PluginValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Configuration
public class PluginCompilerConfig {
    
    @Bean
    public PluginValidator pluginValidator() {
        return new PluginValidator();
    }
    
    @Bean
    public PluginCompiler pluginCompiler(
            @Value("${app.compiler.spigot-api-path}") String spigotApiPath,
            @Value("${app.compiler.visual-runtime-path}") String visualRuntimePath,
            PluginValidator pluginValidator
    ) {
        return new PluginCompiler(
                Path.of(spigotApiPath).toString(),
                Path.of(visualRuntimePath).toString(),
                pluginValidator
        );
    }
}