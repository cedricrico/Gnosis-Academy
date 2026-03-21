CREATE TABLE IF NOT EXISTS admins (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    first_name VARCHAR(128),
    last_name VARCHAR(128),
    email VARCHAR(128),
    password VARCHAR(100) NOT NULL
) ENGINE=InnoDB;

ALTER TABLE admins ADD COLUMN IF NOT EXISTS first_name VARCHAR(128);
ALTER TABLE admins ADD COLUMN IF NOT EXISTS last_name VARCHAR(128);
ALTER TABLE admins ADD COLUMN IF NOT EXISTS email VARCHAR(128);

INSERT INTO admins (username, first_name, last_name, email, password)
VALUES ('admin', 'Admin', 'User', 'admin@school.edu', '$2a$10$zf9bon7rEn7owliwyC7l0O9hrlSrJg/JOnPr0IpwKCKXF4jFsZ8MS')
ON DUPLICATE KEY UPDATE
    first_name = COALESCE(NULLIF(first_name, ''), VALUES(first_name)),
    last_name = COALESCE(NULLIF(last_name, ''), VALUES(last_name)),
    email = COALESCE(NULLIF(email, ''), VALUES(email)),
    password = VALUES(password);

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

CREATE TABLE IF NOT EXISTS school_classes (
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

CREATE TABLE IF NOT EXISTS professors (
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

CREATE TABLE IF NOT EXISTS quiz_attempts (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    quiz_id BIGINT NOT NULL,
    student_id VARCHAR(32) NOT NULL,
    student_name VARCHAR(255) NOT NULL DEFAULT '',
    course VARCHAR(128) NULL,
    section VARCHAR(128) NULL,
    attempt_number INT NOT NULL,
    total_questions INT NOT NULL,
    correct_answers INT NOT NULL,
    answers_json LONGTEXT NOT NULL,
    submitted_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

ALTER TABLE quiz_attempts ADD COLUMN IF NOT EXISTS student_name VARCHAR(255) NOT NULL DEFAULT '';
ALTER TABLE quiz_attempts ADD COLUMN IF NOT EXISTS course VARCHAR(128) NULL;
ALTER TABLE quiz_attempts ADD COLUMN IF NOT EXISTS section VARCHAR(128) NULL;

CREATE TABLE IF NOT EXISTS assignment_submissions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    assignment_id BIGINT NOT NULL,
    assignment_title VARCHAR(200) NOT NULL,
    assignment_subject VARCHAR(200) NULL,
    professor_id VARCHAR(32) NOT NULL,
    student_id VARCHAR(32) NOT NULL,
    student_name VARCHAR(128) NOT NULL,
    student_course VARCHAR(128) NULL,
    student_section VARCHAR(128) NULL,
    assignment_points INT NULL,
    filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(512) NOT NULL,
    content_type VARCHAR(128) NULL,
    file_size BIGINT NULL,
    grade INT NULL,
    feedback LONGTEXT NULL,
    graded_at TIMESTAMP(6) NULL,
    submitted_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_assignment_submission_student UNIQUE (assignment_id, student_id)
) ENGINE=InnoDB;

ALTER TABLE assignment_submissions ADD COLUMN IF NOT EXISTS assignment_points INT NULL;
ALTER TABLE assignment_submissions ADD COLUMN IF NOT EXISTS grade INT NULL;
ALTER TABLE assignment_submissions ADD COLUMN IF NOT EXISTS feedback LONGTEXT NULL;
ALTER TABLE assignment_submissions ADD COLUMN IF NOT EXISTS graded_at TIMESTAMP(6) NULL;
