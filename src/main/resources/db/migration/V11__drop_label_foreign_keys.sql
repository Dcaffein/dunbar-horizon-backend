ALTER TABLE labels DROP FOREIGN KEY fk_labels_owner;

ALTER TABLE label_members
    DROP FOREIGN KEY fk_label_members_label,
    DROP FOREIGN KEY fk_label_members_user;
