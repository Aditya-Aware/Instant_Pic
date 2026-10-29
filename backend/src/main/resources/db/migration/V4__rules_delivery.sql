CREATE TABLE sharing_rules (
    id              UUID PRIMARY KEY,
    event_id        UUID NOT NULL REFERENCES events(id),
    rule_type       VARCHAR(30) NOT NULL,
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    priority        INTEGER NOT NULL DEFAULT 0,
    config_json     JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_sharing_rules_event_type UNIQUE (event_id, rule_type)
);

CREATE INDEX idx_sharing_rules_event ON sharing_rules(event_id);

CREATE TABLE deliveries (
    id                      UUID PRIMARY KEY,
    photo_id                UUID NOT NULL REFERENCES photos(id),
    recipient_member_id     UUID NOT NULL REFERENCES event_members(id),
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reason                  VARCHAR(50) NOT NULL,
    rule_name               VARCHAR(100) NOT NULL,
    decided_at              TIMESTAMPTZ NOT NULL,
    delivered_at            TIMESTAMPTZ,
    retry_count             INTEGER NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ NOT NULL,
    updated_at              TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_deliveries_photo_recipient UNIQUE (photo_id, recipient_member_id)
);

CREATE INDEX idx_deliveries_recipient ON deliveries(recipient_member_id);
CREATE INDEX idx_deliveries_photo ON deliveries(photo_id);
