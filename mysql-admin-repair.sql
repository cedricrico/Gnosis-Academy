-- Repair for MySQL Error 1932: "Table 'gnosis_academy.admins' doesn't exist in engine"
-- Run this in MySQL Workbench while connected to the same server your app uses.

CREATE DATABASE IF NOT EXISTS gnosis_academy;
USE gnosis_academy;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS admins;
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

SELECT id, username FROM admins;
