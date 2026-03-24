package com.example.Gnosis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.chat")
public record ChatProperties(String provider) {
}
