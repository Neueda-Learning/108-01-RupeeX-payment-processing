#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SEED_FILE="$ROOT_DIR/scripts/seed-data.sql"
SCHEMA_FILE="$ROOT_DIR/backend/src/main/resources/schema.sql"
ENV_FILE="$ROOT_DIR/.env"

if [[ ! -f "$SEED_FILE" ]]; then
  echo "Seed SQL not found at $SEED_FILE" >&2
  exit 1
fi

if [[ ! -f "$SCHEMA_FILE" ]]; then
  echo "Schema SQL not found at $SCHEMA_FILE" >&2
  exit 1
fi

if [[ ! -f "$ENV_FILE" ]]; then
  echo ".env not found at $ENV_FILE" >&2
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

if command -v "docker" >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
  COMPOSE_CMD=(docker compose)
elif command -v "docker-compose" >/dev/null 2>&1; then
  COMPOSE_CMD=(docker-compose)
else
  echo "Neither 'docker compose' nor 'docker-compose' is available." >&2
  exit 1
fi

: "${MYSQL_DATABASE:?MYSQL_DATABASE must be set in .env}"
: "${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD must be set in .env}"

run_mysql_query() {
  local query="$1"
  "${COMPOSE_CMD[@]}" exec -T -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" db \
    mysql -N -B -uroot "$MYSQL_DATABASE" -e "$query"
}

ACCOUNTS_TABLE_EXISTS="$(run_mysql_query "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${MYSQL_DATABASE}' AND table_name='accounts';")"

if [[ "$ACCOUNTS_TABLE_EXISTS" != "1" ]]; then
  echo "Schema tables not found in '$MYSQL_DATABASE'. Applying schema first..."
  "${COMPOSE_CMD[@]}" exec -T -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" db \
    mysql -uroot "$MYSQL_DATABASE" < "$SCHEMA_FILE"
fi

echo "Seeding database '$MYSQL_DATABASE' in container 'db'..."
"${COMPOSE_CMD[@]}" exec -T -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" db \
  mysql -uroot "$MYSQL_DATABASE" < "$SEED_FILE"

echo "Seed completed successfully."
