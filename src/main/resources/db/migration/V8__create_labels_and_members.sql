CREATE TABLE labels (
    id VARCHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    owner_id BIGINT NOT NULL,
    name VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_labels_owner_name (owner_id, name),
    CONSTRAINT fk_labels_owner FOREIGN KEY (owner_id) REFERENCES users (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE label_members (
    label_id VARCHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    member_id BIGINT NOT NULL,
    PRIMARY KEY (label_id, member_id),
    KEY idx_label_members_member_label (member_id, label_id),
    CONSTRAINT fk_label_members_label FOREIGN KEY (label_id) REFERENCES labels (id) ON DELETE CASCADE,
    CONSTRAINT fk_label_members_user FOREIGN KEY (member_id) REFERENCES users (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
