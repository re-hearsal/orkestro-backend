CREATE TABLE event_comment (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL,
    author_user_id BIGINT NOT NULL,
    text TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE,
    FOREIGN KEY (author_user_id) REFERENCES users (id),
    CHECK (length(trim(text)) > 0),
    CHECK (length(text) <= 3000)
);

CREATE INDEX idx_event_comment_event_id_created_at
    ON event_comment (event_id, created_at DESC);
