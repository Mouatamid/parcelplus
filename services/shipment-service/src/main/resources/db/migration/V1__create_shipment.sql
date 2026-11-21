CREATE TABLE shipment (
    id              UUID        PRIMARY KEY,
    tracking_code   VARCHAR(20) NOT NULL UNIQUE,
    origin_hub      VARCHAR(10) NOT NULL,
    destination_hub VARCHAR(10) NOT NULL,
    service_level   VARCHAR(20) NOT NULL,
    status          VARCHAR(30) NOT NULL,
    last_sequence   INTEGER     NOT NULL DEFAULT 0,
    version         BIGINT      NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_hubs_differ CHECK (origin_hub <> destination_hub)
);