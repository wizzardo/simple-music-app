CREATE TABLE artist (
    id BIGSERIAL PRIMARY KEY,
    created TIMESTAMP,
    updated TIMESTAMP,
    name VARCHAR(256),
    albums JSONB
)