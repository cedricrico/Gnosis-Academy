package com.example.Gnosis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.openai")
public record OpenAiProperties(
		String apiKey,
		String responsesUrl,
		String model
) {
}
