ALTER TABLE in_app_notification
    ADD COLUMN organization_id BIGINT REFERENCES organization(id) ON DELETE CASCADE;
