#!/bin/sh
set -e

# Ensure data directory exists and has write permissions
mkdir -p /app/data
chown -R node:node /app/data 2>/dev/null || true
chmod 775 /app/data 2>/dev/null || true

# If running as root, switch to node user
if [ "$(id -u)" = '0' ]; then
  exec su-exec node "$@"
fi

exec "$@"

