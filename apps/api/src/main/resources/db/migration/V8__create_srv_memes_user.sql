-- Create restricted service account for the API.
-- Password is supplied via the Flyway placeholder ${srv_memes_password}.
DO $$
BEGIN
    CREATE USER srv_memes WITH PASSWORD '${srv_memes_password}';
EXCEPTION WHEN duplicate_object THEN
    RAISE NOTICE 'Role srv_memes already exists, skipping creation.';
END
$$;

GRANT CONNECT ON DATABASE memesdb TO srv_memes;

GRANT USAGE ON SCHEMA public TO srv_memes;

GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public TO srv_memes;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE ON TABLES TO srv_memes;

GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO srv_memes;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO srv_memes;
