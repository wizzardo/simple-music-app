CREATE TABLE artist (
    id BIGSERIAL PRIMARY KEY,
    created DATE,
    updated DATE,
    name VARCHAR(256),
    albums JSONB
)