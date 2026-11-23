CREATE TABLE buzz_comments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    thread_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    text VARCHAR(300) NOT NULL,
    image_keys JSON NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_buzz_comments_thread (thread_id, id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
