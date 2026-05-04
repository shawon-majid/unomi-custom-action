#!/usr/bin/env bash
set -euo pipefail

UNOMI_URL="${UNOMI_URL:-http://localhost:8181}"
AUTH="${UNOMI_AUTH:-karaf:karaf}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "→ Waiting for Unomi at $UNOMI_URL ..."
for i in $(seq 1 120); do
  code=$(curl -s -o /dev/null -w "%{http_code}" -u "$AUTH" "$UNOMI_URL/cxs/cluster" || true)
  if [[ "$code" == "200" ]]; then
    echo "  Unomi is up (after ${i}s)"
    break
  fi
  if [[ $i -eq 120 ]]; then
    echo "  timed out waiting for Unomi" >&2
    exit 1
  fi
  sleep 1
done

echo "→ Registering scope 'unomi-test' ..."
curl -fsS -u "$AUTH" \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"metadata":{"id":"unomi-test","name":"unomi-test","scope":"unomi-test"}}' \
  "$UNOMI_URL/cxs/scopes" \
  && echo "  ok" \
  || { echo "  scope post failed" >&2; exit 1; }

echo "→ Uploading Groovy action ..."
curl -fsS -u "$AUTH" \
  -F "file=@${SCRIPT_DIR}/notifyOnPageViewMilestoneAction.groovy" \
  "$UNOMI_URL/cxs/groovyActions" \
  && echo "  ok" \
  || { echo "  upload failed" >&2; exit 1; }

echo "→ Posting rule ..."
curl -fsS -u "$AUTH" \
  -X POST \
  -H "Content-Type: application/json" \
  --data @"${SCRIPT_DIR}/rule.json" \
  "$UNOMI_URL/cxs/rules" \
  && echo "  ok" \
  || { echo "  rule post failed" >&2; exit 1; }

echo
echo "✓ Unomi setup complete."
echo "  Tail Unomi logs:  docker compose logs -f unomi"
echo "  Hit the page:     bun run web   →  http://localhost:3000"
echo "  Hook server:      bun run server"
