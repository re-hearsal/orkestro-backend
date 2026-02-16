CREATE TYPE user_language_type AS ENUM ('RU', 'EN');

ALTER TABLE users
ADD COLUMN preferred_language user_language_type NOT NULL DEFAULT 'RU';
