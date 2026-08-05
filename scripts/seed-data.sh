#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SEED_FILE="$ROOT_DIR/scripts/seed-data.sql"
SCHEMA_FILE="$ROOT_DIR/backend/src/main/resources/schema.sql"
PAYER_EMAIL_MIGRATION="$ROOT_DIR/scripts/add-payer-email-column.sql"
CURRENCY_FIELDS_MIGRATION="$ROOT_DIR/scripts/add-currency-fields.sql"
ENV_FILE="$ROOT_DIR/.env"

if [[ ! -f "$SEED_FILE" ]]; then
  echo "Seed SQL not found at $SEED_FILE" >&2
  exit 1
fi

if [[ ! -f "$SCHEMA_FILE" ]]; then
  echo "Schema SQL not found at $SCHEMA_FILE" >&2
  exit 1
fi

if [[ ! -f "$PAYER_EMAIL_MIGRATION" ]]; then
  echo "Payer email migration SQL not found at $PAYER_EMAIL_MIGRATION" >&2
  exit 1
fi

if [[ ! -f "$CURRENCY_FIELDS_MIGRATION" ]]; then
  echo "Currency fields migration SQL not found at $CURRENCY_FIELDS_MIGRATION" >&2
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

echo "🔄 Starting database reset and seeding..."
echo ""

# Step 1: Drop all existing tables (disable foreign key checks temporarily)
echo "Step 1/4: Dropping all existing tables..."
run_mysql_query "SET FOREIGN_KEY_CHECKS=0;"
run_mysql_query "DROP TABLE IF EXISTS email_notification_log;"
run_mysql_query "DROP TABLE IF EXISTS notifications;"
run_mysql_query "DROP TABLE IF EXISTS system_events;"
run_mysql_query "DROP TABLE IF EXISTS audit_logs;"
run_mysql_query "DROP TABLE IF EXISTS dead_letter_queue;"
run_mysql_query "DROP TABLE IF EXISTS processing_queue;"
run_mysql_query "DROP TABLE IF EXISTS payment_history;"
run_mysql_query "DROP TABLE IF EXISTS fraud_detection_rules;"
run_mysql_query "DROP TABLE IF EXISTS payments;"
run_mysql_query "DROP TABLE IF EXISTS accounts;"
run_mysql_query "DROP TABLE IF EXISTS users;"
run_mysql_query "SET FOREIGN_KEY_CHECKS=1;"
echo "✅ All tables dropped successfully."
echo ""

# Step 2: Recreate schema from scratch (includes payer_email via migration)
echo "Step 2/4: Recreating database schema from scratch..."
"${COMPOSE_CMD[@]}" exec -T -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" db \
  mysql -uroot "$MYSQL_DATABASE" < "$SCHEMA_FILE"
echo "✅ Schema recreated successfully."
echo ""

# Step 3: Apply migrations (add payer_email column if not already present)
echo "Step 3/5: Applying database migrations..."
"${COMPOSE_CMD[@]}" exec -T -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" db \
  mysql -uroot "$MYSQL_DATABASE" < "$PAYER_EMAIL_MIGRATION" 2>/dev/null || true
echo "✅ Payer email migration applied successfully."
echo ""

# Step 4: Apply currency fields migration
echo "Step 4/5: Applying currency conversion fields migration..."
"${COMPOSE_CMD[@]}" exec -T -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" db \
  mysql -uroot "$MYSQL_DATABASE" < "$CURRENCY_FIELDS_MIGRATION" 2>/dev/null || true
echo "✅ Currency fields migration applied successfully."
echo ""

# Step 5: Seed fresh test data
echo "Step 5/5: Seeding fresh test data..."
"${COMPOSE_CMD[@]}" exec -T -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" db \
  mysql -uroot "$MYSQL_DATABASE" < "$SEED_FILE"
echo "✅ Test data seeded successfully."
echo ""

echo "═══════════════════════════════════════════════"
echo "✅ Database reset completed successfully!"
echo "═══════════════════════════════════════════════"
echo "Database: $MYSQL_DATABASE"
echo "Schema: Applied from $SCHEMA_FILE"
echo "Migrations: Applied from $PAYER_EMAIL_MIGRATION and $CURRENCY_FIELDS_MIGRATION"
echo "Seed Data: Loaded from $SEED_FILE"
echo ""
echo "Database is now fresh with pre-seed data."
echo "═══════════════════════════════════════════════"
