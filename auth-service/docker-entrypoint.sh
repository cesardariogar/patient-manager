#!/bin/sh
set -e

# If secret file exists, read it into the env var
if [ -f /run/secrets/AUTH_DB_PASS ]; then
  export SPRING_DATASOURCE_PASSWORD=$(cat /run/secrets/AUTH_DB_PASS)
fi

if [ -f /run/secrets/JWT_SECRET ]; then
  export JWT_SECRET=$(cat /run/secrets/JWT_SECRET)
fi

exec java -jar app.jar
