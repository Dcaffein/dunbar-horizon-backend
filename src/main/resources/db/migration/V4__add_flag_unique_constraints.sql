-- 제약을 걸기 전에 기존 중복을 지운다. 가장 먼저 만들어진 행만 남긴다.
DELETE dup FROM flag_invitations dup
JOIN flag_invitations keep
  ON dup.flag_id = keep.flag_id
 AND dup.invitee_id = keep.invitee_id
 AND dup.id > keep.id;

DELETE dup FROM flag_participants dup
JOIN flag_participants keep
  ON dup.flag_id = keep.flag_id
 AND dup.participant_id = keep.participant_id
 AND dup.id > keep.id;

ALTER TABLE flag_invitations
    DROP INDEX idx_flag_invitations_flag_invitee,
    ADD UNIQUE KEY uq_flag_invitations_flag_invitee (flag_id, invitee_id);

ALTER TABLE flag_participants
    DROP INDEX idx_flag_participants_flag_participant,
    ADD UNIQUE KEY uq_flag_participants_flag_participant (flag_id, participant_id);
