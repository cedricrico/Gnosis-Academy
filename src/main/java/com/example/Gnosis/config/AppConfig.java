package com.example.Gnosis.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({
		OpenAiProperties.class,
		OllamaProperties.class,
		ChatProperties.class
})
public class AppConfig {

	@Bean
	public RestClient restClient() {
		return RestClient.builder().build();
	}

	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}
}
