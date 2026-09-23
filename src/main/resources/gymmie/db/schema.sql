CREATE TABLE IF NOT EXISTS account (
    id INTEGER PRIMARY KEY,
    username TEXT NOT NULL COLLATE NOCASE UNIQUE,
    password_hash TEXT NOT NULL,
    salt TEXT NOT NULL,
    display_name TEXT NOT NULL,
    role TEXT NOT NULL,
    active INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1))
);

CREATE TABLE IF NOT EXISTS membership_plan (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    duration_days INTEGER NOT NULL,
    price_cents INTEGER NOT NULL,
    archived INTEGER NOT NULL DEFAULT 0 CHECK (archived IN (0, 1))
);

CREATE TABLE IF NOT EXISTS membership (
    id INTEGER PRIMARY KEY,
    member_id INTEGER NOT NULL,
    plan_id INTEGER NOT NULL,
    start_date TEXT NOT NULL,
    expiry_date TEXT NOT NULL,
    status TEXT NOT NULL,
    snapshot_price_cents INTEGER NOT NULL,
    snapshot_duration_days INTEGER NOT NULL,
    FOREIGN KEY (member_id) REFERENCES account (id),
    FOREIGN KEY (plan_id) REFERENCES membership_plan (id)
);

CREATE TABLE IF NOT EXISTS training_session (
    id INTEGER PRIMARY KEY,
    trainer_id INTEGER NOT NULL,
    starts_at TEXT NOT NULL,
    duration_minutes INTEGER NOT NULL,
    capacity INTEGER NOT NULL,
    description TEXT,
    cancelled INTEGER NOT NULL DEFAULT 0 CHECK (cancelled IN (0, 1)),
    FOREIGN KEY (trainer_id) REFERENCES account (id)
);

CREATE TABLE IF NOT EXISTS booking (
    id INTEGER PRIMARY KEY,
    session_id INTEGER NOT NULL,
    member_id INTEGER NOT NULL,
    booked_at TEXT NOT NULL,
    status TEXT NOT NULL,
    cancellation_reason TEXT,
    UNIQUE (session_id, member_id),
    FOREIGN KEY (session_id) REFERENCES training_session (id),
    FOREIGN KEY (member_id) REFERENCES account (id)
);
