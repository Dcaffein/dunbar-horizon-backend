CREATE VIEW active_users AS
SELECT user_id, nickname, profile_image
FROM users
WHERE status = 'ACTIVE';
