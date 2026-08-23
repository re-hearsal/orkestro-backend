-- Backfill organization_id and normalize entity_type on notifications created before 013 added the column.

UPDATE in_app_notification
SET organization_id = entity_id
WHERE entity_type = 'organization' AND organization_id IS NULL;

UPDATE in_app_notification
SET entity_type = 'ORGANIZATION'
WHERE entity_type = 'organization';

UPDATE in_app_notification n
SET organization_id = e.organization_id
FROM events e
WHERE n.entity_type = 'EVENT' AND n.entity_id = e.id AND n.organization_id IS NULL;

UPDATE in_app_notification n
SET organization_id = t.organization_id
FROM task t
WHERE n.entity_type = 'TASK' AND n.entity_id = t.id AND n.organization_id IS NULL;
