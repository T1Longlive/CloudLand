#!/usr/bin/env bash
set -Eeuo pipefail

APP_DIR="/root/cloudland"
BRANCH="${1:-main}"
LOCK_FILE="/tmp/cloudland-deploy.lock"
LOG_FILE="/var/log/cloudland-deploy.log"
FRONTEND_CONTAINER="cloudland-frontend"
FRONTEND_PORT="80"

log() {
  echo "[$(date '+%F %T')] $*" | tee -a "$LOG_FILE"
}

port_in_use() {
  ss -lnt "( sport = :$FRONTEND_PORT )" 2>/dev/null | grep -q ":$FRONTEND_PORT"
}

show_port_owner() {
  ss -lntp | grep ":$FRONTEND_PORT" || true
}

stop_host_nginx() {
  if command -v systemctl >/dev/null 2>&1; then
    if systemctl list-unit-files nginx.service >/dev/null 2>&1; then
      if systemctl is-active --quiet nginx; then
        log "Stopping host nginx service"
        systemctl stop nginx
      fi
      if systemctl is-enabled nginx >/dev/null 2>&1; then
        log "Disabling host nginx service"
        systemctl disable nginx >/dev/null 2>&1 || true
      fi
    fi
  fi

  if pgrep -x nginx >/dev/null 2>&1; then
    log "Stopping remaining host nginx processes"
    pkill -x nginx || true
    sleep 2
  fi
}

ensure_frontend_port_available() {
  if ! port_in_use; then
    return 0
  fi

  log "Port $FRONTEND_PORT is in use, attempting to stop host nginx"
  show_port_owner | tee -a "$LOG_FILE" >/dev/null
  stop_host_nginx

  if port_in_use; then
    log "Port $FRONTEND_PORT is still occupied after stopping host nginx"
    show_port_owner | tee -a "$LOG_FILE" >/dev/null
    exit 1
  fi

  log "Port $FRONTEND_PORT is now available"
}

wait_for_frontend() {
  local retries=12
  local delay=5
  local status=""

  for _ in $(seq 1 "$retries"); do
    status="$(docker inspect -f '{{.State.Status}}' "$FRONTEND_CONTAINER" 2>/dev/null || true)"
    if [ "$status" = "running" ]; then
      log "Frontend container is running"
      return 0
    fi
    sleep "$delay"
  done

  log "Frontend container failed to reach running state, current status: ${status:-missing}"
  $DC ps | tee -a "$LOG_FILE" >/dev/null || true
  $DC logs --tail=200 frontend | tee -a "$LOG_FILE" >/dev/null || true
  exit 1
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

ensure_frontend_port_available

log "Starting containers"
$DC up -d --remove-orphans

wait_for_frontend

log "Container status"
$DC ps

log "Cleaning unused images"
docker image prune -f >/dev/null 2>&1 || true

log "Deployment finished"
