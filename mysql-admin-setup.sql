CREATE TABLE IF NOT EXISTS admins (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL
);

INSERT INTO admins (username, password)
VALUES ('admin', '$2a$10$zf9bon7rEn7owliwyC7l0O9hrlSrJg/JOnPr0IpwKCKXF4jFsZ8MS')
ON DUPLICATE KEY UPDATE
    password = VALUES(password);
