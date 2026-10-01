#!/bin/bash
# Client for bridge.sh (use from the sandbox VM): q.sh adb shell input tap 500 800
#   q.sh screencap name   -> outbox/name.png ; q.sh gradle :app:installDebug
D="$(cd "$(dirname "$0")" && pwd)"; id="r$(date +%s%N)"
echo "$*" > "$D/inbox/$id.tmp" && mv "$D/inbox/$id.tmp" "$D/inbox/$id.req"
for i in $(seq 1 ${QWAIT:-1700}); do
  if [ -f "$D/outbox/$id.out" ] && tail -1 "$D/outbox/$id.out" | grep -qE 'EXIT [0-9]+$'; then
    cat "$D/outbox/$id.out"; rm -f "$D/outbox/$id.out" "$D/outbox/$id.req.done" 2>/dev/null; exit 0; fi
  sleep 0.1
done; echo "TIMEOUT waiting for bridge (is bridge.sh running on the Mac?)"; exit 1
