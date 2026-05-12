CREATE TABLE org_info_message (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organization(id),
    section_id BIGINT REFERENCES sections(id),
    author_user_id BIGINT NOT NULL REFERENCES users(id),
    text VARCHAR(5000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_org_info_message_org_created ON org_info_message (organization_id, created_at DESC);
