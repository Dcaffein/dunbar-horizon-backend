ALTER TABLE buzzes
    ADD COLUMN deleted_at DATETIME(6) NULL AFTER edited_at,
    ADD KEY idx_buzzes_deleted (deleted_at);
