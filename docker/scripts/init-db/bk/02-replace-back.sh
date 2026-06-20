#!/bin/bash
set -e

echo "Restoring SQL initialization scripts..."

# Paths to secrets
PATIENT_PASS=$(cat /run/secrets/PATIENT_DB_PASSWORD)
AUTH_PASS=$(cat /run/secrets/AUTH_DB_PASSWORD)

# Replace placeholders inside SQL scripts
sed -i "s/${PATIENT_PASS}/REPLACE_PATIENT_PASSWORD/g" /docker-entrypoint-initdb.d/01-init.sql
sed -i "s/${AUTH_PASS}/REPLACE_AUTH_PASSWORD/g" /docker-entrypoint-initdb.d/01-init.sql

echo "Init scripts variables restored."