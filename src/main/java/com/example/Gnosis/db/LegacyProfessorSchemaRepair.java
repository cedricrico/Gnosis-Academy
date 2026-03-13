package com.example.Gnosis.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import java.sql.DatabaseMetaData;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * Best-effort runtime repair for legacy MySQL schemas where old columns still exist on the `professors` table
 * (e.g. `age`) and are NOT NULL without a default, causing inserts to fail.
 *
 * Hibernate's ddl-auto=update will not drop/relax such legacy constraints, so we patch them here.
 */
@Component
public class LegacyProfessorSchemaRepair {
	private static final Logger log = LoggerFactory.getLogger(LegacyProfessorSchemaRepair.class);

	private final JdbcTemplate jdbcTemplate;

	public LegacyProfessorSchemaRepair(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void repair() {
		if (!isMySql()) {
			return;
		}

		// Columns seen in older schemas that can block inserts if left NOT NULL without defaults.
		makeColumnNullableIfBlockingInsert("professors", "age");
		makeColumnNullableIfBlockingInsert("professors", "sex");
		makeColumnNullableIfBlockingInsert("professors", "position");
		makeColumnNullableIfBlockingInsert("professors", "professor_id");
	}

	private boolean isMySql() {
		try {
			if (jdbcTemplate.getDataSource() == null) {
				return false;
			}
			try (Connection connection = jdbcTemplate.getDataSource().getConnection()) {
				DatabaseMetaData meta = connection.getMetaData();
				String product = meta != null ? meta.getDatabaseProductName() : null;
				return product != null && product.toLowerCase().contains("mysql");
			}
		} catch (Exception ex) {
			// If we can't detect it, don't risk running DDL.
			log.debug("Unable to determine database product, skipping legacy professor schema repair.", ex);
			return false;
		}
	}

	private void makeColumnNullableIfBlockingInsert(String tableName, String columnName) {
		try {
			List<Map<String, Object>> rows = jdbcTemplate.queryForList(
					"""
					SELECT column_type, is_nullable, column_default
					FROM information_schema.columns
					WHERE table_schema = DATABASE()
					  AND table_name = ?
					  AND column_name = ?
					LIMIT 1
					""",
					tableName,
					columnName
			);

			if (rows.isEmpty()) {
				return;
			}

			Map<String, Object> row = rows.get(0);
			String columnType = row.get("column_type") != null ? row.get("column_type").toString() : null;
			String isNullable = row.get("is_nullable") != null ? row.get("is_nullable").toString() : null;
			Object columnDefault = row.get("column_default");

			if (columnType == null || isNullable == null) {
				return;
			}

			// The problematic case: NOT NULL with no default means inserts must supply a value.
			if (!"NO".equalsIgnoreCase(isNullable) || columnDefault != null) {
				return;
			}

			String sql = "ALTER TABLE " + tableName + " MODIFY COLUMN " + columnName + " " + columnType + " NULL";
			jdbcTemplate.execute(sql);
			log.warn("Applied legacy schema repair: {}.{} changed to NULLABLE to unblock inserts.", tableName, columnName);
		} catch (Exception ex) {
			// Best-effort: don't take the app down if DDL fails.
			log.warn("Legacy schema repair skipped/failed for {}.{}.", tableName, columnName, ex);
		}
	}
}
