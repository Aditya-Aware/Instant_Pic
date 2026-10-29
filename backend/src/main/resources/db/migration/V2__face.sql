CREATE TABLE face_profiles (
    id                          UUID PRIMARY KEY,
    event_member_id             UUID NOT NULL UNIQUE REFERENCES event_members(id),
    embedding                   BYTEA NOT NULL,
    embedding_model_version     VARCHAR(50) NOT NULL,
    sample_count                INTEGER NOT NULL,
    status                      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at                  TIMESTAMPTZ NOT NULL,
    updated_at                  TIMESTAMPTZ NOT NULL
);
