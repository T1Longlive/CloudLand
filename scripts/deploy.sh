#!/usr/bin/env bash
set -Eeuo pipefail

APP_DIR="/root/cloudland"
BRANCH="${1:-main}"
LOCK_FILE="/tmp/cloudland-deploy.lock"
LOG_FILE="/var/log/cloudland-deploy.log"

log() {
  echo "[$(date '+%F %T')] $*" | tee -a "$LOG_FILE"
}

exec 9>"$LOCK_FILE"
if ! flock -n 9; then
  log "Another deployment is already running, exiting"
  exit 1
fi

cd "$APP_DIR"

if docker compose version >/dev/null 2>&1; then
  DC="docker compose"
elif command -v docker-compose >/dev/null 2>&1; then
  DC="docker-compose"
else
  log "docker compose is not installed"
  exit 1
fi

log "Starting deployment for branch: $BRANCH"

git fetch origin "$BRANCH"
git checkout "$BRANCH"
git reset --hard "origin/$BRANCH"

log "Pulling dependency images"
$DC pull mysql redis || true

log "Building application images"
$DC build --pull backend frontend

log "Starting containers"
$DC up -d --remove-orphans

log "Container status"
$DC ps

log "Cleaning unused images"
docker image prune -f >/dev/null 2>&1 || true

log "Deployment finished"
