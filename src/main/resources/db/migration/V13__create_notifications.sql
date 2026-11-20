CREATE TABLE notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    receiver_id BIGINT,
    title VARCHAR(255) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    metadata JSON,
    type VARCHAR(30) NOT NULL,
    is_read BOOLEAN NOT NULL,
    is_sent BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_notifications_receiver_created (receiver_id, created_at),
    KEY idx_notifications_created (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
