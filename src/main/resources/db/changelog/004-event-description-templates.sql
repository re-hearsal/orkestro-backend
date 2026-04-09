CREATE TABLE event_description_template (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
    event_type event_type_type NOT NULL,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    created_by_user_id BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
