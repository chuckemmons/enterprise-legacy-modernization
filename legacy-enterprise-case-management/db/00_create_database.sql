-- Run as a PostgreSQL administrator; change the placeholder password.
-- psql -U postgres -f db/00_create_database.sql
CREATE USER legacy_app WITH PASSWORD 'REPLACE_WITH_SECURE_PASSWORD';
CREATE DATABASE legacy_cases OWNER legacy_app ENCODING 'UTF8';
-- Run db/01_schema.sql as legacy_app inside legacy_cases after database creation.
