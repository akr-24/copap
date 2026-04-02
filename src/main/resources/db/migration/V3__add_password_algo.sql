-- V3: Graceful BCrypt migration
-- Existing users have SHA-256 hashed passwords (no salt).
-- New column tracks which algorithm was used so AuthService can
-- verify existing users with SHA-256 and re-hash with BCrypt on login.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS password_algo VARCHAR(10) NOT NULL DEFAULT 'sha256';
