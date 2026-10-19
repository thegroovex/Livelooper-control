#!/usr/bin/env bash
# Background CI waiter (token-saving): tools/ci-wait.sh <run_id> [repo]
# Polls every 60 s for up to ~18 min, prints "status conclusion"; on failure prints failing jobs.
# Run with github credentials (curl to api.github.com; no gh CLI in the sandbox).
run=$1; repo=${2:-thegroovex/Livelooper-control}; api=https://api.github.com/repos/$repo
end=$(( $(date +%s) + 1100 ))
while [ "$(date +%s)" -lt $end ]; do
  s=$(curl -s "$api/actions/runs/$run" | python3 -c "import json,sys;d=json.load(sys.stdin);print(d.get('status'),d.get('conclusion'))")
  case "$s" in completed*)
    echo "$s"
    [ "$s" = "completed success" ] || curl -s "$api/actions/runs/$run/jobs" | python3 -c "import json,sys;[print('FAILED',j['id'],j['name']) for j in json.load(sys.stdin)['jobs'] if j['conclusion'] not in ('success','skipped')]"
    exit 0;; esac
  sleep 60
done
echo "timeout $s"
