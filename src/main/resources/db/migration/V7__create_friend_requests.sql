CREATE TABLE friend_requests (
    low_user_id BIGINT NOT NULL,
    high_user_id BIGINT NOT NULL,
    requester_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    status VARCHAR(10) NOT NULL,
    PRIMARY KEY (low_user_id, high_user_id),
    KEY idx_friend_requests_sent (requester_id, status),
    KEY idx_friend_requests_received (receiver_id, status),
    KEY idx_friend_requests_cleanup (status, created_at),
    CONSTRAINT chk_friend_requests_pair CHECK (
        low_user_id < high_user_id
        AND low_user_id = LEAST(requester_id, receiver_id)
        AND high_user_id = GREATEST(requester_id, receiver_id)
    ),
    CONSTRAINT chk_friend_requests_status CHECK (status IN ('PENDING', 'HIDDEN')),
    CONSTRAINT fk_friend_requests_requester FOREIGN KEY (requester_id) REFERENCES users (user_id),
    CONSTRAINT fk_friend_requests_receiver FOREIGN KEY (receiver_id) REFERENCES users (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
