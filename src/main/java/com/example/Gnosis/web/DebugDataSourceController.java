package com.example.Gnosis.web;

import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

@RestController
@RequestMapping("/professor/api/debug")
public class DebugDataSourceController {
	private final Environment environment;
	private final DataSource dataSource;

	public DebugDataSourceController(Environment environment, DataSource dataSource) {
		this.environment = environment;
		this.dataSource = dataSource;
	}

	@GetMapping("/datasource")
	public DataSourceInfo datasource() {
		String url = environment.getProperty("spring.datasource.url");
		String username = environment.getProperty("spring.datasource.username");
		String activeProfiles = String.join(",", environment.getActiveProfiles());
		String jdbcUrl = null;
		String catalog = null;

		try (Connection connection = dataSource.getConnection()) {
			DatabaseMetaData metaData = connection.getMetaData();
			jdbcUrl = metaData.getURL();
			catalog = connection.getCatalog();
		} catch (Exception ignored) {
		}

		return new DataSourceInfo(activeProfiles, url, jdbcUrl, username, catalog);
	}

	private record DataSourceInfo(
			String activeProfiles,
			String configuredUrl,
			String jdbcUrl,
			String username,
			String catalog
	) {}
}
