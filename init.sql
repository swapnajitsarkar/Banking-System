-- MySQL initialization script
-- Runs automatically when the Docker container first starts

CREATE DATABASE IF NOT EXISTS bankingdb
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE bankingdb;

-- InnoDB is set as default to guarantee ACID compliance.
-- MyISAM does NOT support transactions and must never be used in banking systems.
SET default_storage_engine = InnoDB;

-- Seed an admin user (password: Admin@123 — BCrypt hash, change before production)
-- The application will create tables via Hibernate ddl-auto=update on first boot.
-- This seed runs AFTER the schema is created.
-- INSERT INTO users (full_name, email, password, role, enabled, created_at)
-- VALUES ('System Admin', 'admin@banking.com',
--         '$2a$12$...YOUR_BCRYPT_HASH_HERE...', 'ROLE_ADMIN', true, NOW());
