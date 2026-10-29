CREATE TABLE users (
    id              UUID PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(120) NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL
);

CREATE TABLE events (
    id                  UUID PRIMARY KEY,
    name                VARCHAR(200) NOT NULL,
    description         TEXT,
    created_by_user_id  UUID NOT NULL REFERENCES users(id),
    start_date          DATE,
    end_date            DATE,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_events_created_by ON events(created_by_user_id);

CREATE TABLE event_members (
    id              UUID PRIMARY KEY,
    event_id        UUID NOT NULL REFERENCES events(id),
    user_id         UUID NOT NULL REFERENCES users(id),
    display_name    VARCHAR(120) NOT NULL,
    role            VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    joined_at       TIMESTAMPTZ NOT NULL,
    left_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_event_members_event_user UNIQUE (event_id, user_id)
);

CREATE INDEX idx_event_members_event ON event_members(event_id);
CREATE INDEX idx_event_members_user ON event_members(user_id);

CREATE TABLE invitations (
    id                      UUID PRIMARY KEY,
    event_id                UUID NOT NULL REFERENCES events(id),
    code                    VARCHAR(32) NOT NULL UNIQUE,
    created_by_member_id    UUID NOT NULL REFERENCES event_members(id),
    expires_at              TIMESTAMPTZ NOT NULL,
    max_uses                INTEGER,
    uses_count              INTEGER NOT NULL DEFAULT 0,
    status                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at              TIMESTAMPTZ NOT NULL,
    updated_at              TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_invitations_event ON invitations(event_id);
