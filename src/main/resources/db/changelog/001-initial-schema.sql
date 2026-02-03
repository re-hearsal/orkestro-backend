CREATE TYPE notification_channel_type AS ENUM ('TELEGRAM', 'EMAIL');

CREATE TYPE visibility_level_type AS ENUM ('PUBLIC', 'PRIVATE');

CREATE TYPE file_type_type AS ENUM ('PDF', 'PHOTO', 'AUDIO', 'VIDEO', 'OTHER');

CREATE TYPE link_type_type AS ENUM ('WEBSITE', 'YOUTUBE', 'INSTAGRAM', 'FACEBOOK', 'TELEGRAM', 'OTHER');

CREATE TYPE organization_user_status_type AS ENUM ('PENDING', 'ACCEPTED', 'REJECTED');

CREATE TYPE event_type_type AS ENUM ('REHEARSAL', 'CONCERT', 'OTHER');

CREATE TYPE event_rsvp_status_type AS ENUM ('PENDING', 'ACCEPTED', 'DECLINED');

CREATE TYPE event_attendance_status_type AS ENUM ('UNKNOWN', 'ATTENDED', 'ABSENT', 'EXCUSED');

CREATE TYPE event_participant_source_type AS ENUM ('ORGANIZATION', 'SECTION', 'MANUAL');

CREATE TYPE task_status_type AS ENUM ('OPEN', 'IN_PROGRESS', 'DONE', 'CANCELLED');

CREATE TYPE task_visibility_type AS ENUM ('ALL_MEMBERS', 'ROLE_RESTRICTED');

CREATE TYPE role_scope_type AS ENUM ('ORGANIZATION', 'SECTION');

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username TEXT NOT NULL,
    name TEXT NOT NULL,
    email TEXT NOT NULL CHECK (email LIKE '%@%'),
    password TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    telegram_user_id BIGINT,
    notification_channel_id notification_channel_type NOT NULL DEFAULT 'EMAIL',
    location TEXT,
    birth_date DATE,
    profile_image_file_id BIGINT
);

CREATE TABLE file (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    file_type file_type_type NOT NULL,
    bucket_name TEXT NOT NULL,
    object_name TEXT NOT NULL,
    size BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    uploaded_by BIGINT REFERENCES users (id),
    UNIQUE (bucket_name, object_name)
);

CREATE TABLE instrument (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    icon_key TEXT NOT NULL,
    UNIQUE (name, icon_key)
);

INSERT INTO
    instrument (name, icon_key)
VALUES
    ('Conductor', 'conductor'),
    -- Strings
    ('Violin', 'violin'),
    ('Viola', 'viola'),
    ('Cello', 'cello'),
    ('Double bass', 'double-bass'),
    -- Woodwinds
    ('Flute', 'flute'),
    ('Alto flute', 'alto-flute'),
    ('Piccolo', 'piccolo'),
    ('Oboe', 'oboe'),
    ('English horn', 'english-horn'),
    ('Clarinet', 'clarinet'),
    ('Bass clarinet', 'bass-clarinet'),
    ('Bassoon', 'bassoon'),
    ('Contrabassoon', 'contrabassoon'),
    -- Saxophones
    ('Soprano saxophone', 'soprano-saxophone'),
    ('Alto saxophone', 'alto-saxophone'),
    ('Tenor saxophone', 'tenor-saxophone'),
    ('Baritone saxophone', 'baritone-saxophone'),
    -- Brass
    ('Trumpet', 'trumpet'),
    ('Cornet', 'cornet'),
    ('Flugelhorn', 'flugelhorn'),
    ('French horn', 'french-horn'),
    ('Trombone', 'trombone'),
    ('Bass trombone', 'bass-trombone'),
    ('Tuba', 'tuba'),
    ('Euphonium', 'euphonium'),
    -- Percussion
    ('Percussion', 'percussion'),
    ('Triangle', 'triangle'),
    ('Tam-tam', 'tam-tam'),
    ('Timpani', 'timpani'),
    ('Xylophone', 'xylophone'),
    ('Marimba', 'marimba'),
    ('Vibraphone', 'vibraphone'),
    ('Glockenspiel', 'glockenspiel'),
    ('Tubular bells', 'tubular-bells'),
    -- Keyboards and plucked
    ('Piano', 'piano'),
    ('Harpsichord', 'harpsichord'),
    ('Organ', 'organ'),
    ('Harp', 'harp'),
    ('Guitar', 'guitar'),
    ('Electric guitar', 'electric-guitar'),
    ('Bass guitar', 'bass-guitar'),
    ('Synthesizer', 'synthesizer'),
    ('Accordion', 'accordion'),
    -- Choir / voices
    ('Choir soprano', 'choir-soprano'),
    ('Choir alto', 'choir-alto'),
    ('Choir tenor', 'choir-tenor'),
    ('Choir bass', 'choir-bass');

CREATE TABLE users_instrument (
    user_id BIGINT NOT NULL,
    instrument_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, instrument_id),
    FOREIGN KEY (user_id) REFERENCES users (id),
    FOREIGN KEY (instrument_id) REFERENCES instrument (id)
);

CREATE TABLE organization (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    location TEXT NOT NULL,
    description TEXT,
    profile_image_file_id BIGINT REFERENCES file (id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    visibility_level visibility_level_type NOT NULL DEFAULT 'PUBLIC',
    UNIQUE (name)
);

CREATE TABLE organization_links (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    link_type link_type_type NOT NULL,
    url TEXT NOT NULL,
    FOREIGN KEY (organization_id) REFERENCES organization (id),
    UNIQUE (
        organization_id,
        link_type,
        url
    )
);

CREATE TABLE organization_users (
    organization_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status organization_user_status_type NOT NULL,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (organization_id, user_id),
    FOREIGN KEY (organization_id) REFERENCES organization (id),
    FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE sections (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    organization_id BIGINT NOT NULL,
    parent_section_id BIGINT REFERENCES sections (id),
    FOREIGN KEY (organization_id) REFERENCES organization (id),
    FOREIGN KEY (parent_section_id) REFERENCES sections (id),
    UNIQUE (
        name,
        organization_id,
        parent_section_id
    )
);

CREATE TABLE section_users (
    section_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (section_id, user_id),
    FOREIGN KEY (section_id) REFERENCES sections (id),
    FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE organization_invite (
    organization_id BIGINT PRIMARY KEY,
    code TEXT NOT NULL UNIQUE,
    created_by_user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (organization_id) REFERENCES organization (id),
    FOREIGN KEY (created_by_user_id) REFERENCES users (id)
);

CREATE TABLE song (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    title TEXT NOT NULL,
    composer TEXT,
    duration_seconds INT,
    description TEXT,
    video_url TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (organization_id) REFERENCES organization (id),
    UNIQUE (organization_id, title)
);

CREATE TABLE song_instruments (
    song_id BIGINT NOT NULL,
    instrument_id BIGINT NOT NULL,
    count INT NOT NULL,
    PRIMARY KEY (song_id, instrument_id, count),
    FOREIGN KEY (song_id) REFERENCES song (id) ON DELETE CASCADE,
    FOREIGN KEY (instrument_id) REFERENCES instrument (id)
);

CREATE TABLE song_files (
    song_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    PRIMARY KEY (song_id, file_id),
    FOREIGN KEY (song_id) REFERENCES song (id) ON DELETE CASCADE,
    FOREIGN KEY (file_id) REFERENCES file (id)
);

CREATE TABLE permission (
    code TEXT PRIMARY KEY,
    description TEXT NOT NULL
);

CREATE TABLE role (
    id BIGSERIAL PRIMARY KEY,
    scope role_scope_type NOT NULL,
    organization_id BIGINT NULL REFERENCES organization (id),
    section_id BIGINT NULL REFERENCES sections (id),
    name TEXT NOT NULL,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (
        (
            scope = 'ORGANIZATION'
            AND section_id IS NULL
            AND (
                organization_id IS NOT NULL
                OR is_system
            )
        )
        OR (
            scope = 'SECTION'
            AND organization_id IS NULL
            AND (
                section_id IS NOT NULL
                OR is_system
            )
        )
    ),
    UNIQUE (
        scope,
        organization_id,
        section_id,
        name
    )
);

CREATE TABLE role_permission (
    role_id BIGINT NOT NULL,
    permission_code TEXT NOT NULL,
    PRIMARY KEY (role_id, permission_code),
    FOREIGN KEY (role_id) REFERENCES role (id) ON DELETE CASCADE,
    FOREIGN KEY (permission_code) REFERENCES permission (code) ON DELETE CASCADE
);

CREATE TABLE user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (role_id) REFERENCES role (id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    creator_user_id BIGINT NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    event_type event_type_type NOT NULL,
    external_link TEXT,
    location TEXT,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    send_rsvp BOOLEAN NOT NULL DEFAULT FALSE,
    remind_before_minutes INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (organization_id) REFERENCES organization (id),
    FOREIGN KEY (creator_user_id) REFERENCES users (id),
    CHECK (end_time > start_time)
);

CREATE TABLE event_tags (
    event_id BIGINT NOT NULL,
    tag TEXT NOT NULL,
    PRIMARY KEY (event_id, tag),
    FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE
);

CREATE TABLE event_participants (
    event_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    source event_participant_source_type NOT NULL,
    rsvp_status event_rsvp_status_type NOT NULL DEFAULT 'PENDING',
    attendance_status event_attendance_status_type NOT NULL DEFAULT 'UNKNOWN',
    rsvp_at TIMESTAMP,
    PRIMARY KEY (event_id, user_id),
    FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE event_files (
    event_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    PRIMARY KEY (event_id, file_id),
    FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE,
    FOREIGN KEY (file_id) REFERENCES file (id)
);

CREATE TABLE event_songs (
    event_id BIGINT NOT NULL,
    song_id BIGINT NOT NULL,
    position INT,
    PRIMARY KEY (event_id, song_id),
    FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE,
    FOREIGN KEY (song_id) REFERENCES song (id)
);

CREATE TABLE event_participant_songs (
    event_id BIGINT NOT NULL,
    song_id BIGINT NOT NULL,
    instrument_id BIGINT NOT NULL,
    position INT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (
        event_id,
        song_id,
        instrument_id,
        position
    ),
    FOREIGN KEY (event_id, user_id) REFERENCES event_participants (event_id, user_id) ON DELETE CASCADE,
    FOREIGN KEY (song_id) REFERENCES song (id),
    FOREIGN KEY (instrument_id) REFERENCES instrument (id),
    UNIQUE (
        event_id,
        song_id,
        instrument_id,
        user_id
    )
);

CREATE TABLE task (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    section_id BIGINT,
    title TEXT NOT NULL,
    description TEXT,
    author_user_id BIGINT NOT NULL,
    assignee_user_id BIGINT,
    status task_status_type NOT NULL DEFAULT 'OPEN',
    visibility task_visibility_type NOT NULL DEFAULT 'ALL_MEMBERS',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP,
    FOREIGN KEY (organization_id) REFERENCES organization (id),
    FOREIGN KEY (section_id) REFERENCES sections (id),
    FOREIGN KEY (author_user_id) REFERENCES users (id),
    FOREIGN KEY (assignee_user_id) REFERENCES users (id)
);

CREATE TABLE task_files (
    task_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    PRIMARY KEY (task_id, file_id),
    FOREIGN KEY (task_id) REFERENCES task (id) ON DELETE CASCADE,
    FOREIGN KEY (file_id) REFERENCES file (id)
);

CREATE TABLE task_comment (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,
    author_user_id BIGINT NOT NULL,
    text TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES task (id) ON DELETE CASCADE,
    FOREIGN KEY (author_user_id) REFERENCES users (id)
);

CREATE TABLE task_visibility_role (
    task_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (task_id, role_id),
    FOREIGN KEY (task_id) REFERENCES task (id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES role (id) ON DELETE CASCADE
);


CREATE TABLE user_telegram_link_token (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token TEXT NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    used_at TIMESTAMP
);

CREATE UNIQUE INDEX ux_users_telegram_user_id ON users (telegram_user_id);

ALTER TABLE users
ADD CONSTRAINT fk_users_profile_image_file FOREIGN KEY (profile_image_file_id) REFERENCES file (id);

INSERT INTO
    permission (code, description)
VALUES (
        'ORG_DELETE',
        'Delete organizations'
    ),
    (
        'ORG_EDIT',
        'Edit organization name, location, description, profile image, etc.'
    ),
    (
        'ORG_SET_VISIBILITY',
        'Set and change visibility level (PUBLIC / PRIVATE)'
    ),
    (
        'ORG_MEMBER_INVITE',
        'Invite/add users to organization'
    ),
    (
        'ORG_MEMBER_REMOVE',
        'Remove users from organization'
    ),
    (
        'ORG_JOIN_REQUEST_VIEW',
        'View join requests to organization'
    ),
    (
        'ORG_JOIN_REQUEST_MANAGE',
        'Approve/reject join requests'
    ),
    (
        'ORG_ASSIGN_TECH_ROLE',
        'Assign technical roles to users'
    ),
    (
        'ORG_TECH_ROLE_MANAGE',
        'Create/update/delete technical roles and their permissions in organization'
    ),
    (
        'SECTION_CREATE',
        'Create sections and nested sections'
    ),
    (
        'SECTION_EDIT',
        'Edit section parameters'
    ),
    (
        'SECTION_DELETE',
        'Delete sections'
    ),
    (
        'SECTION_MEMBER_ADD',
        'Add users to section'
    ),
    (
        'SECTION_MEMBER_REMOVE',
        'Remove users from section'
    ),
    (
        'SECTION_ASSIGN_TECH_ROLE',
        'Assign technical roles in section'
    ),
    (
        'SECTION_TECH_ROLE_MANAGE',
        'Manage roles and permissions at section level'
    ),
    (
        'REPERTOIRE_CREATE_SONG',
        'Create songs'
    ),
    (
        'REPERTOIRE_EDIT_SONG',
        'Edit songs'
    ),
    (
        'REPERTOIRE_DELETE_SONG',
        'Delete songs'
    ),
    (
        'REPERTOIRE_MANAGE_FILES',
        'Manage sheet music files (PDF, images) and audio for songs'
    ),
    (
        'REPERTOIRE_MANAGE_TAGS',
        'Manage song tags'
    ),
    (
        'REPERTOIRE_MANAGE_INSTRUMENTATION',
        'Set and edit instrumentation for song'
    ),
    (
        'EVENT_MARK_ATTENDANCE',
        'Mark attendance of event participants'
    ),
    (
        'EVENT_DELETION',
        'Delete events'
    ),
    ('TASK_MANAGE', 'Create tasks');

-- Basic system roles at organization level (templates)
INSERT INTO
    role (
        scope,
        organization_id,
        section_id,
        name,
        is_system
    )
VALUES (
        'ORGANIZATION',
        NULL,
        NULL,
        'Leader',
        TRUE
    ),
    (
        'ORGANIZATION',
        NULL,
        NULL,
        'Co-leader',
        TRUE
    );

-- Assume these are the first two roles in an empty DB:
-- id = 1 -> Leader, id = 2 -> Co-leader
INSERT INTO
    role_permission (role_id, permission_code)
VALUES
    -- Leader: full current permission set
    (1, 'ORG_DELETE'),
    (1, 'ORG_EDIT'),
    (1, 'ORG_SET_VISIBILITY'),
    (1, 'ORG_MEMBER_INVITE'),
    (1, 'ORG_MEMBER_REMOVE'),
    (1, 'ORG_JOIN_REQUEST_VIEW'),
    (1, 'ORG_JOIN_REQUEST_MANAGE'),
    (1, 'ORG_ASSIGN_TECH_ROLE'),
    (1, 'ORG_TECH_ROLE_MANAGE'),
    (1, 'SECTION_CREATE'),
    (1, 'SECTION_EDIT'),
    (1, 'SECTION_DELETE'),
    (1, 'SECTION_MEMBER_ADD'),
    (1, 'SECTION_MEMBER_REMOVE'),
    (1, 'SECTION_ASSIGN_TECH_ROLE'),
    (1, 'SECTION_TECH_ROLE_MANAGE'),
    (1, 'REPERTOIRE_CREATE_SONG'),
    (1, 'REPERTOIRE_EDIT_SONG'),
    (1, 'REPERTOIRE_DELETE_SONG'),
    (1, 'REPERTOIRE_MANAGE_FILES'),
    (1, 'REPERTOIRE_MANAGE_TAGS'),
    (
        1,
        'REPERTOIRE_MANAGE_INSTRUMENTATION'
    ),
    (1, 'EVENT_MARK_ATTENDANCE'),
    (1, 'EVENT_DELETION'),
    (1, 'TASK_MANAGE'),
    -- Co-leader: reduced permission set
    (2, 'ORG_EDIT'),
    (2, 'ORG_MEMBER_INVITE'),
    (2, 'ORG_MEMBER_REMOVE'),
    (2, 'ORG_JOIN_REQUEST_VIEW'),
    (2, 'ORG_JOIN_REQUEST_MANAGE'),
    (2, 'ORG_ASSIGN_TECH_ROLE'),
    (2, 'SECTION_CREATE'),
    (2, 'SECTION_EDIT'),
    (2, 'SECTION_MEMBER_ADD'),
    (2, 'SECTION_MEMBER_REMOVE'),
    (2, 'SECTION_ASSIGN_TECH_ROLE'),
    (2, 'REPERTOIRE_CREATE_SONG'),
    (2, 'REPERTOIRE_EDIT_SONG'),
    (2, 'REPERTOIRE_DELETE_SONG'),
    (2, 'REPERTOIRE_MANAGE_FILES'),
    (2, 'REPERTOIRE_MANAGE_TAGS'),
    (
        2,
        'REPERTOIRE_MANAGE_INSTRUMENTATION'
    ),
    (2, 'EVENT_MARK_ATTENDANCE'),
    (2, 'EVENT_DELETION'),
    (2, 'TASK_MANAGE');

-- Basic system roles at section level (templates)
INSERT INTO
    role (
        scope,
        organization_id,
        section_id,
        name,
        is_system
    )
VALUES (
        'SECTION',
        NULL,
        NULL,
        'Leader',
        TRUE
    ),
    (
        'SECTION',
        NULL,
        NULL,
        'Co-leader',
        TRUE
    );

-- Assume these are the next two roles in an empty DB:
-- id = 3 -> Section Leader, id = 4 -> Section Co-leader
INSERT INTO
    role_permission (role_id, permission_code)
VALUES
    -- Section Leader: full section permission set
    (3, 'SECTION_CREATE'),
    (3, 'SECTION_EDIT'),
    (3, 'SECTION_DELETE'),
    (3, 'SECTION_MEMBER_ADD'),
    (3, 'SECTION_MEMBER_REMOVE'),
    (3, 'SECTION_ASSIGN_TECH_ROLE'),
    (3, 'SECTION_TECH_ROLE_MANAGE'),
    -- Section Co-leader: reduced section permission set
    (4, 'SECTION_CREATE'),
    (4, 'SECTION_EDIT'),
    (4, 'SECTION_MEMBER_ADD'),
    (4, 'SECTION_MEMBER_REMOVE'),
    (4, 'SECTION_ASSIGN_TECH_ROLE');