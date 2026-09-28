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

mkdir -p /opt/nutri
