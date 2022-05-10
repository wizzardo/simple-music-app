CREATE TABLE config (
    id BIGSERIAL PRIMARY KEY,
    created TIMESTAMP,
    updated TIMESTAMP,
    name VARCHAR(256),
    data JSONB
)