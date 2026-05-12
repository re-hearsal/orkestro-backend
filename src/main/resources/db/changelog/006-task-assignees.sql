CREATE TABLE task_assignee (
    task_id BIGINT NOT NULL REFERENCES task(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    PRIMARY KEY (task_id, user_id)
);

ALTER TABLE task DROP COLUMN IF EXISTS assignee_user_id;
