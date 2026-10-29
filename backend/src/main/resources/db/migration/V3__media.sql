CREATE TABLE photos (
    id                      UUID PRIMARY KEY,
    event_id                UUID NOT NULL REFERENCES events(id),
    uploaded_by_member_id   UUID NOT NULL REFERENCES event_members(id),
    storage_key             VARCHAR(255) NOT NULL,
    thumbnail_key           VARCHAR(255),
    captured_at             TIMESTAMPTZ,
    width                   INTEGER,
    height                  INTEGER,
    checksum                VARCHAR(64) NOT NULL,
    processing_status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    gallery_visible         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMPTZ NOT NULL,
    updated_at              TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_photos_event ON photos(event_id);

CREATE TABLE photo_faces (
    id                      UUID PRIMARY KEY,
    photo_id                UUID NOT NULL REFERENCES photos(id),
    bounding_box_x          INTEGER NOT NULL,
    bounding_box_y          INTEGER NOT NULL,
    bounding_box_w          INTEGER NOT NULL,
    bounding_box_h          INTEGER NOT NULL,
    detection_confidence    DOUBLE PRECISION NOT NULL,
    embedding               BYTEA NOT NULL,
    created_at              TIMESTAMPTZ NOT NULL,
    updated_at              TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_photo_faces_photo ON photo_faces(photo_id);

CREATE TABLE face_matches (
    id                      UUID PRIMARY KEY,
    photo_face_id           UUID NOT NULL REFERENCES photo_faces(id),
    event_member_id         UUID NOT NULL REFERENCES event_members(id),
    face_profile_id         UUID NOT NULL REFERENCES face_profiles(id),
    similarity_score        DOUBLE PRECISION NOT NULL,
    match_status            VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    matched_at              TIMESTAMPTZ NOT NULL,
    created_at              TIMESTAMPTZ NOT NULL,
    updated_at              TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_face_matches_photo_face ON face_matches(photo_face_id);
CREATE INDEX idx_face_matches_member ON face_matches(event_member_id);
