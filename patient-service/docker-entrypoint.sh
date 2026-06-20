#!/bin/sh
set -e

# If secret file exists, read it into the env var
if [ -f /run/secrets/PATIENT_DB_PASS ]; then
  export SPRING_DATASOURCE_PASSWORD=$(cat /run/secrets/PATIENT_DB_PASS)
fi

exec java -jar app.jar
