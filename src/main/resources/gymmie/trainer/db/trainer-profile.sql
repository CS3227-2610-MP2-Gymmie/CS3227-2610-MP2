-- Executed by SchemaInitializer against the application's runtime SQLite connection.
-- noinspection SqlNoDataSourceInspection
CREATE TABLE IF NOT EXISTS trainer_profile (
    account_id INTEGER PRIMARY KEY REFERENCES account (id),
    synopsis TEXT NOT NULL DEFAULT ''
);

-- noinspection SqlNoDataSourceInspection
CREATE TABLE IF NOT EXISTS trainer_specialization (
    account_id INTEGER NOT NULL REFERENCES trainer_profile (account_id),
    position INTEGER NOT NULL CHECK (position >= 0),
    tag TEXT NOT NULL CHECK (length(tag) > 0),
    PRIMARY KEY (account_id, position)
);
