CREATE TABLE in_app_notification (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type        TEXT      NOT NULL,
    title       TEXT      NOT NULL,
    body        TEXT,
    entity_id   BIGINT,
    entity_type TEXT,
    is_read     BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_in_app_notification_user_created ON in_app_notification (user_id, created_at DESC);
