ALTER TABLE users
    ADD COLUMN verified_operation_account BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_user_verified_operation_account
    ON users(verified_operation_account);
