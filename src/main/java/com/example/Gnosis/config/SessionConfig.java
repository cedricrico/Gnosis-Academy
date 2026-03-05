package com.example.Gnosis.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;

/**
 * Forces Spring Session to back HttpSession with JDBC (SPRING_SESSION tables).
 * Boot often auto-configures this when spring-session-jdbc is present, but this
 * removes ambiguity when debugging.
 */
@Configuration
@EnableJdbcHttpSession
public class SessionConfig {
}

