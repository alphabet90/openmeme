CREATE TABLE api_keys (
    id          BIGSERIAL PRIMARY KEY,
    key_hash    VARCHAR(64)  NOT NULL UNIQUE,
    client_name VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL CHECK (role IN ('READ', 'WRITE', 'ADMIN')),
    active      BOOLEAN      NOT NULL DEFAULT true,
    expires_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_used_at TIMESTAMPTZ
);

CREATE INDEX idx_api_keys_key_hash ON api_keys(key_hash);
CREATE INDEX idx_api_keys_active ON api_keys(active) WHERE active = true;

-- Grant to restricted service account (same pattern as V8)
GRANT SELECT, INSERT, UPDATE ON api_keys TO srv_memes;
GRANT USAGE, SELECT ON SEQUENCE api_keys_id_seq TO srv_memes;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE ON TABLES TO srv_memes;
