package de.jkueck.monitor.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

@Configuration
public class ObjectMapperConfig {

    @Bean
    public ObjectMapper jsonObjectMapper() {
        return JsonMapper.builder().build();
    }

    @Bean
    public ObjectMapper yamlObjectMapper() {
        return YAMLMapper.builder().build();
    }

}
