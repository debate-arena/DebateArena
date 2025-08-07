#!/bin/bash
# Simplified wait-for-it.sh that works without external packages

set -e

host="$1"
port="$2"
timeout="${3:-30}"

echo "Waiting for $host:$port..."

for i in $(seq 1 $timeout); do
    if timeout 1 bash -c "echo >/dev/tcp/$host/$port" 2>/dev/null; then
        echo "$host:$port is available!"
        exit 0
    fi
    echo "Waiting... ($i/$timeout)"
    sleep 1
done

echo "Timeout waiting for $host:$port"
exit 1