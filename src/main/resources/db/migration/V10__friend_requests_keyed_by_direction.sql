ALTER TABLE friend_requests
    DROP CHECK chk_friend_requests_pair,
    DROP PRIMARY KEY,
    ADD PRIMARY KEY (requester_id, receiver_id),
    DROP INDEX idx_friend_requests_sent,
    DROP INDEX idx_friend_requests_cleanup,
    DROP COLUMN low_user_id,
    DROP COLUMN high_user_id,
    ADD UNIQUE INDEX uk_friend_requests_pair ((LEAST(requester_id, receiver_id)), (GREATEST(requester_id, receiver_id)));
