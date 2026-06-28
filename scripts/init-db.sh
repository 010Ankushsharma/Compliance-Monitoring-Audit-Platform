#!/bin/bash
# =============================================================
#  PostgreSQL initialisation script
#  File: scripts/init-db.sh
#  Runs once on first container start (docker-entrypoint-initdb.d)
# =============================================================
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" << 'ENDSQL'
-- Create read-only role for reporting tools
DO $$ BEGIN
  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'compliance_readonly') THEN
    CREATE ROLE compliance_readonly;
  END IF;
END $$;

GRANT CONNECT ON DATABASE compliance_db TO compliance_readonly;
GRANT USAGE ON SCHEMA public TO compliance_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT ON TABLES TO compliance_readonly;

-- Create retention role (can bypass immutability trigger via DB perms)
DO $$ BEGIN
  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'compliance_data_retention') THEN
    CREATE ROLE compliance_data_retention;
  END IF;
END $$;

-- Enable extensions
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
CREATE EXTENSION IF NOT EXISTS pgcrypto;
ENDSQL

echo "Database initialisation complete"
