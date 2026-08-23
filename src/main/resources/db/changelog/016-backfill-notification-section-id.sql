-- Backfill section_id on NEW_INFO_MESSAGE notifications created before 015 added the column.

UPDATE in_app_notification n
SET section_id = m.section_id
FROM org_info_message m
WHERE n.entity_type = 'ORG_INFO_MESSAGE' AND n.entity_id = m.id AND n.section_id IS NULL;
