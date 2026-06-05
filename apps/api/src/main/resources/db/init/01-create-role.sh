#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    DO \$\$
    BEGIN
        IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'memes') THEN
            CREATE ROLE memes WITH LOGIN PASSWORD '${DB_PASSWORD:-memes}';
        END IF;
    END
    $$;

    \c memesdb

    GRANT CONNECT ON DATABASE memesdb TO memes;
    GRANT USAGE ON SCHEMA public TO memes;
    GRANT CREATE ON SCHEMA public TO memes;
    ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO memes;
EOSQL
