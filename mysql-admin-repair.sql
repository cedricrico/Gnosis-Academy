-- Repair for MySQL Error 1932: "Table 'gnosis_academy.admins' doesn't exist in engine"
-- Run this in MySQL Workbench while connected to the same server your app uses.

CREATE DATABASE IF NOT EXISTS gnosis_academy;
USE gnosis_academy;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS admins;
DROP TABLE IF EXISTS professors;
DROP TABLE IF EXISTS school_classes;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE admins (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL
) ENGINE=InnoDB;

INSERT INTO admins (username, password)
VALUES ('admin', '$2a$10$zf9bon7rEn7owliwyC7l0O9hrlSrJg/JOnPr0IpwKCKXF4jFsZ8MS')
ON DUPLICATE KEY UPDATE
    password = VALUES(password);

CREATE TABLE school_classes (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    course_code VARCHAR(32) NOT NULL,
    course_name VARCHAR(128) NOT NULL,
    course VARCHAR(128) NOT NULL,
    section_name VARCHAR(128) NOT NULL,
    subject_name VARCHAR(128) NOT NULL,
    subject_code VARCHAR(64) NOT NULL,
    professor_id VARCHAR(32) NOT NULL,
    schedule_days VARCHAR(64) NOT NULL,
    start_time VARCHAR(16) NOT NULL,
    end_time VARCHAR(16) NOT NULL,
    status VARCHAR(32) NOT NULL,
    subjects_json LONGTEXT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

CREATE TABLE professors (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    employee_id VARCHAR(32) NOT NULL UNIQUE,
    first_name VARCHAR(128) NOT NULL,
    middle_initial VARCHAR(1),
    last_name VARCHAR(128) NOT NULL,
    email VARCHAR(128) NOT NULL UNIQUE,
    department VARCHAR(64) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

SELECT id, username FROM admins;
