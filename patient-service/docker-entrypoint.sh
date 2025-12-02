#!/bin/sh
set -e

# If secret file exists, read it into the env var
if [ -f /run/secrets/DB_PATIENT_PASSWORD ]; then
  export SPRING_DATASOURCE_PASSWORD=$(cat /run/secrets/DB_PATIENT_PASSWORD)
fi

exec java -jar app.jar
