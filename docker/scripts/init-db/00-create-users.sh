#!/bin/bash
set -e

echo "Creating users from Docker secrets..."

PATIENT_PASS=$(cat /run/secrets/PATIENT_DB_PASS)
AUTH_PASS=$(cat /run/secrets/AUTH_DB_PASS)

psql -v ON_ERROR_STOP=1 \
     --username "$POSTGRES_USER" \
     --dbname "$POSTGRES_DB" <<EOF

CREATE USER patient_admin
WITH PASSWORD '${PATIENT_PASS}';

CREATE USER auth_admin
WITH PASSWORD '${AUTH_PASS}';

EOF

echo "Users created."