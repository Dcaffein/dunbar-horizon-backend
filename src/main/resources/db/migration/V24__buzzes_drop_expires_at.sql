ALTER TABLE buzzes
    DROP KEY idx_buzzes_expires,
    DROP COLUMN expires_at,
    ADD KEY idx_buzzes_created (created_at);
