package overskam.projectM.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.amqp.autoconfigure.RabbitTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {
    public static final String BUILD_REQUEST_QUEUE = "projectm.build.request";
    
    @Bean
    public Queue buildRequestQueue() {
        return new Queue(BUILD_REQUEST_QUEUE, true);
    }
    
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
    
    @Bean
    public RabbitTemplateCustomizer rabbitTemplateCustomizer(MessageConverter jsonMessageConverter) {
        return rabbitTemplate -> rabbitTemplate.setMessageConverter(jsonMessageConverter);
    }
}