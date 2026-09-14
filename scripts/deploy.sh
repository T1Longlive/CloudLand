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

ensure_host_nginx_stopped() {
  if ! port_in_use; then
    return 0
  fi

  log "Port $FRONTEND_PORT is in use before deploy"
  show_port_owner | tee -a "$LOG_FILE" >/dev/null
  stop_host_nginx

  if port_in_use; then
    log "Port $FRONTEND_PORT remains in use after stopping host nginx, continuing because docker compose may be replacing the existing frontend container"
    show_port_owner | tee -a "$LOG_FILE" >/dev/null
    return 0
  fi

  log "Port $FRONTEND_PORT is available after stopping host nginx"
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

# ---------- 应用镜像来自云端构建产物（方案 B）----------
# 镜像由 Gitee 云端构建（build@docker）并推送到阿里云 ACR，
# 服务器只负责拉取运行，不再本地编译 —— 因此不再需要 Maven / Node，
# 也就没有了本地编译的内存峰值（本机内存仅 1.8G，曾被 OOM 杀掉过）。
ACR_HOST="crpi-jzqyelh9p5zb0dmu.cn-chengdu.personal.cr.aliyuncs.com"

# 私有仓库需要先登录。若没登录，pull 会报 Authentication 相关错误，
# 这里提前检测并给出可执行的提示，避免对着难懂的报错排查。
if [ ! -f "$HOME/.docker/config.json" ] || ! grep -q "$ACR_HOST" "$HOME/.docker/config.json" 2>/dev/null; then
  log "WARN: 未检测到 $ACR_HOST 的登录记录"
  log "      若 pull 失败，请先执行： docker login $ACR_HOST"
fi

log "Pulling application images from registry"
$DC pull backend frontend

ensure_host_nginx_stopped

log "Starting containers"
$DC up -d --remove-orphans --force-recreate backend frontend mysql redis

wait_for_frontend

log "Container status"
$DC ps

log "Cleaning unused images"
docker image prune -f >/dev/null 2>&1 || true

log "Deployment finished"
