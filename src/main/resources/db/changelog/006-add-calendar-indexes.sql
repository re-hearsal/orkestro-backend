CREATE INDEX idx_events_org_start_id ON events (organization_id, start_time, id);

CREATE INDEX idx_events_org_end_id ON events (organization_id, end_time, id);

CREATE INDEX idx_event_sections_section_event ON event_sections (section_id, event_id);

CREATE INDEX idx_event_participants_user_event ON event_participants (user_id, event_id);
