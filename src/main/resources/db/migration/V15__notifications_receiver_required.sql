DELETE FROM notifications WHERE receiver_id IS NULL;

ALTER TABLE notifications MODIFY receiver_id BIGINT NOT NULL;
