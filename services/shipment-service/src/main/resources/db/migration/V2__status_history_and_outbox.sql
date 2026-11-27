-- One row per status the shipment has had. sequence_no matches shipment.last_sequence, CREATED = 0.
CREATE TABLE shipment_status_history (
    id          UUID        PRIMARY KEY,
    shipment_id UUID        NOT NULL REFERENCES shipment (id),
    sequence_no INTEGER     NOT NULL,
    status      VARCHAR(30) NOT NULL,
    hub_code    VARCHAR(10) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_history_shipment_sequence UNIQUE (shipment_id, sequence_no)
);

-- Transactional outbox. id is the event's eventId, so a consumer can deduplicate on it.
-- aggregate_id has no foreign key on purpose: the outbox is a log, not part of the shipment's data model.
CREATE TABLE outbox (
    id           UUID        PRIMARY KEY,
    aggregate_id UUID        NOT NULL,
    event_type   VARCHAR(50) NOT NULL,
    payload      JSONB       NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ
);

-- The poller only reads unpublished rows. A partial index stays tiny because published rows leave it.
CREATE INDEX idx_outbox_unpublished ON outbox (created_at) WHERE published_at IS NULL;
