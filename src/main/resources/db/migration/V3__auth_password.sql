ALTER TABLE users
    ADD COLUMN IF NOT EXISTS password_hash text,
    ADD COLUMN IF NOT EXISTS status text NOT NULL default 'ACTIVE' check (status in ('ACTIVE', 'LOCKED', 'DISABLED')),
    ADD COLUMN IF NOT EXISTS failed_attempts int NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_login_at timestamptz;

CREATE UNIQUE INDEX IF NOT EXISTS ux_users_email ON users(lower(email));