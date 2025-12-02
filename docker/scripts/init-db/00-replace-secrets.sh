#!/bin/bash
set -e

echo "Injecting Docker secrets into SQL initialization scripts..."

# Paths to secrets
PATIENT_PASS=$(cat /run/secrets/DB_PATIENT_PASSWORD)
AUTH_PASS=$(cat /run/secrets/DB_AUTH_PASSWORD)

# Replace placeholders inside SQL scripts
sed -i "s/REPLACE_PATIENT_PASSWORD/${PATIENT_PASS}/g" /docker-entrypoint-initdb.d/01-init.sql
sed -i "s/REPLACE_AUTH_PASSWORD/${AUTH_PASS}/g" /docker-entrypoint-initdb.d/01-init.sql

echo "Secrets injected successfully."

exec "$@"