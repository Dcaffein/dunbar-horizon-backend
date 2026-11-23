DELETE FROM buzz_comments;
DELETE FROM buzz_threads;
DELETE FROM buzz_audience_members;
DELETE FROM buzzes;

ALTER TABLE buzzes MODIFY id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE buzz_audience_members MODIFY buzz_id BIGINT NOT NULL;
ALTER TABLE buzz_threads MODIFY buzz_id BIGINT NOT NULL;
