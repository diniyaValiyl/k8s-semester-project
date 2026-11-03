#!/bin/sh
set -eu

: "${DB_HOST:?DB_HOST is required}"
: "${DB_PORT:?DB_PORT is required}"
: "${POSTGRES_DB:?POSTGRES_DB is required}"
: "${POSTGRES_USER:?POSTGRES_USER is required}"
: "${POSTGRES_PASSWORD:?POSTGRES_PASSWORD is required}"

export PGPASSWORD="${POSTGRES_PASSWORD}"

until pg_isready \
  --host="${DB_HOST}" \
  --port="${DB_PORT}" \
  --username="${POSTGRES_USER}" \
  --dbname="${POSTGRES_DB}"; do
  sleep 2
done

psql \
  --host="${DB_HOST}" \
  --port="${DB_PORT}" \
  --username="${POSTGRES_USER}" \
  --dbname="${POSTGRES_DB}" \
  --set=ON_ERROR_STOP=1 <<'SQL'
BEGIN;

SELECT pg_advisory_xact_lock(730031);

CREATE TABLE IF NOT EXISTS database_init_history (
    name VARCHAR(100) PRIMARY KEY,
    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

SELECT NOT EXISTS (
    SELECT 1
    FROM database_init_history
    WHERE name = 'baseline-v1'
) AS should_initialize \gset

\if :should_initialize
    SELECT to_regclass('public.clinics') IS NULL AS database_is_empty \gset

    \if :database_is_empty
        \ir /db-init/init.sql
        INSERT INTO database_init_history (name) VALUES ('baseline-v1');
        \echo 'Database schema and demo data initialized.'
    \else
        INSERT INTO database_init_history (name) VALUES ('baseline-v1');
        \echo 'Existing database detected; initialization marked as complete.'
    \endif
\else
    \echo 'Database is already initialized.'
\endif

COMMIT;
SQL
