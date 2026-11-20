SET @unique_index = (
    SELECT INDEX_NAME
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'device_tokens'
      AND COLUMN_NAME = 'fcm_token'
      AND NON_UNIQUE = 0
    LIMIT 1
);
SET @drop_unique_index = IF(@unique_index IS NULL, 'DO 0',
    CONCAT('ALTER TABLE device_tokens DROP INDEX `', @unique_index, '`'));
PREPARE drop_unique_index FROM @drop_unique_index;
EXECUTE drop_unique_index;
DEALLOCATE PREPARE drop_unique_index;

ALTER TABLE device_tokens
    DROP COLUMN id,
    CHANGE COLUMN fcm_token token VARCHAR(255) NOT NULL,
    ADD PRIMARY KEY (token);
