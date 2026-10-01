#!/usr/bin/env bash
# GCE startup script (ADR-013). Runs on every boot; idempotent.
set -euo pipefail

# 1 GB swap: e2-micro has 1 GB RAM, pip install in the image build needs more.
if ! swapon --show | grep -q /swapfile; then
  if [ ! -f /swapfile ]; then
    fallocate -l 1G /swapfile
    chmod 600 /swapfile
    mkswap /swapfile
  fi
  swapon /swapfile
  grep -q '^/swapfile ' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

if ! command -v docker >/dev/null 2>&1 || ! docker compose version >/dev/null 2>&1; then
  export DEBIAN_FRONTEND=noninteractive
  apt-get update -y
  apt-get install -y docker.io docker-compose-v2
fi
systemctl enable --now docker

mkdir -p /opt/nutri /opt/nutri/logs
chmod 700 /opt/nutri/logs

# Dev conversation log: 30 days by age (CP5). copytruncate: the server keeps the file open
# in append mode. lastaction also ages out the server's own size backups (.1 .. .5).
cat > /etc/logrotate.d/nutri <<'LOGROTATE'
/opt/nutri/logs/conversations.jsonl {
	su root root
	daily
	rotate 30
	maxage 30
	dateext
	dateformat -%Y%m%d
	copytruncate
	compress
	delaycompress
	missingok
	notifempty
	lastaction
		find /opt/nutri/logs -maxdepth 1 -type f -name 'conversations.jsonl.[0-9]*' -mtime +30 -delete
	endscript
}
LOGROTATE
chmod 644 /etc/logrotate.d/nutri
