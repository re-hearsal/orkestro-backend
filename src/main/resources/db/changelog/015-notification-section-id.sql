ALTER TABLE in_app_notification
    ADD COLUMN section_id BIGINT REFERENCES sections(id) ON DELETE CASCADE;
