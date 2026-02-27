INSERT INTO permission (code, description)
VALUES ('EVENT_WRITE_COMMENT', 'Write comments on events');

INSERT INTO role_permission (role_id, permission_code)
VALUES
    (1, 'EVENT_WRITE_COMMENT'),
    (2, 'EVENT_WRITE_COMMENT');
