CREATE TABLE buzzes (
    id VARCHAR(36) NOT NULL,
    creator_id BIGINT NOT NULL,
    text VARCHAR(1000) NOT NULL,
    image_keys JSON NOT NULL,
    created_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_buzzes_creator_created (creator_id, created_at),
    KEY idx_buzzes_expires (expires_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE buzz_audience_members (
    buzz_id VARCHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    is_read BOOLEAN NOT NULL,
    PRIMARY KEY (buzz_id, user_id),
    KEY idx_buzz_audience_members_user (user_id, buzz_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
