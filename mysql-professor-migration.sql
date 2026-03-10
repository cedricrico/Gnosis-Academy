-- One-time migration to align the `professors` table with the new employee-based schema.
-- Run this against your MySQL database (gnosis_academy or the configured schema).

SET @db := DATABASE();

-- Add employee_id column if missing.
SET @sql := (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = @db
              AND table_name = 'professors'
              AND column_name = 'employee_id'
        ),
        'SELECT 1',
        'ALTER TABLE professors ADD COLUMN employee_id VARCHAR(32) NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- If old professor_id exists, copy it into employee_id.
SET @sql := (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = @db
              AND table_name = 'professors'
              AND column_name = 'professor_id'
        ),
        'UPDATE professors SET employee_id = professor_id WHERE employee_id IS NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Fill any remaining null employee_id values.
UPDATE professors
SET employee_id = CONCAT('EMP', LPAD(id, 6, '0'))
WHERE employee_id IS NULL;

-- Add email column if missing.
SET @sql := (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = @db
              AND table_name = 'professors'
              AND column_name = 'email'
        ),
        'SELECT 1',
        'ALTER TABLE professors ADD COLUMN email VARCHAR(128) NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Backfill emails if missing (unique placeholder).
UPDATE professors
SET email = CONCAT(employee_id, '@school.edu')
WHERE email IS NULL OR email = '';

-- Ensure required constraints.
ALTER TABLE professors
    MODIFY COLUMN employee_id VARCHAR(32) NOT NULL,
    MODIFY COLUMN email VARCHAR(128) NOT NULL;

-- Add unique indexes if missing.
SET @sql := (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = @db
              AND table_name = 'professors'
              AND index_name = 'uq_professors_employee_id'
        ),
        'SELECT 1',
        'ALTER TABLE professors ADD UNIQUE KEY uq_professors_employee_id (employee_id)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = @db
              AND table_name = 'professors'
              AND index_name = 'uq_professors_email'
        ),
        'SELECT 1',
        'ALTER TABLE professors ADD UNIQUE KEY uq_professors_email (email)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Drop legacy columns if they exist.
SET @sql := (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = @db
              AND table_name = 'professors'
              AND column_name = 'age'
        ),
        'ALTER TABLE professors DROP COLUMN age',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = @db
              AND table_name = 'professors'
              AND column_name = 'sex'
        ),
        'ALTER TABLE professors DROP COLUMN sex',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = @db
              AND table_name = 'professors'
              AND column_name = 'position'
        ),
        'ALTER TABLE professors DROP COLUMN position',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = @db
              AND table_name = 'professors'
              AND column_name = 'professor_id'
        ),
        'ALTER TABLE professors DROP COLUMN professor_id',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
