package com.example.Gnosis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ollama")
public record OllamaProperties(
		String baseUrl,
		String chatUrl,
		String model
) {
}
