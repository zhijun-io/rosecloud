#!/usr/bin/env bash
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COLLECTION="$DIR/rosecloud-auth-system.postman_collection.json"
ENVIRONMENT="$DIR/rosecloud-local.postman_environment.json"

if ! command -v newman >/dev/null 2>&1; then
  echo "newman not found on PATH; running via npx (will download if needed)..."
  exec npx --yes newman run "$COLLECTION" --environment "$ENVIRONMENT" --color on
fi

newman run "$COLLECTION" --environment "$ENVIRONMENT" --color on
