-- Student registration schema alignment for MySQL.
-- Use this if your database already exists and "users" table is missing.

CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    student_id VARCHAR(32) NOT NULL UNIQUE,
    first_name VARCHAR(128) NOT NULL,
    middle_initial VARCHAR(1),
    last_name VARCHAR(128) NOT NULL,
    age INT NULL DEFAULT 18,
    sex VARCHAR(16) NULL DEFAULT 'other',
    course VARCHAR(128),
    section_name VARCHAR(128),
    status VARCHAR(32),
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;
