ALTER TABLE events
ADD COLUMN include_all_organization_members BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE event_sections (
    event_id BIGINT NOT NULL,
    section_id BIGINT NOT NULL,
    PRIMARY KEY (event_id, section_id),
    FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE,
    FOREIGN KEY (section_id) REFERENCES sections (id)
);
