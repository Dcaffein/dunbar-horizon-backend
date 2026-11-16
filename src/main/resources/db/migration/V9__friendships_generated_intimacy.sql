ALTER TABLE friendships MODIFY COLUMN intimacy DOUBLE
    GENERATED ALWAYS AS (SQRT(
        (GREATEST(low_interest_score, 0) / (GREATEST(low_interest_score, 0) + 50))
        * (GREATEST(high_interest_score, 0) / (GREATEST(high_interest_score, 0) + 50))
    )) STORED NOT NULL;
