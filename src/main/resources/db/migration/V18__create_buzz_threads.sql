CREATE TABLE buzz_threads (
    id BIGINT NOT NULL AUTO_INCREMENT,
    buzz_id VARCHAR(36) NOT NULL,
    starter_id BIGINT NOT NULL,
    visibility VARCHAR(10) NOT NULL,
    text VARCHAR(300) NOT NULL,
    image_keys JSON NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_buzz_threads_buzz_starter_visibility (buzz_id, starter_id, visibility)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
