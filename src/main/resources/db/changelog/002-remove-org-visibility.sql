DELETE FROM role_permission
WHERE permission_code = 'ORG_SET_VISIBILITY';

DELETE FROM permission
WHERE code = 'ORG_SET_VISIBILITY';

ALTER TABLE organization
DROP COLUMN visibility_level;

DROP TYPE IF EXISTS visibility_level_type;
