#!/usr/bin/env bash
# Recreate the working copies after a sandbox reset.
set -e
W=/workspace/work
mkdir -p $W/llc
[ -d $W/llc/repo/.git ] || git clone https://github.com/thegroovex/Livelooper-control $W/llc/repo
[ -d $W/livelooper/.git ] || git clone -b v1.7-wip https://github.com/thegroovex/livelooper $W/livelooper
for r in $W/llc/repo $W/livelooper; do git -C $r config user.name thegroovex; git -C $r config user.email groovex@gmail.com; done
python3 -c "import PIL" 2>/dev/null || pip install --quiet pillow
echo "ready – read docs/dev/RESUME.md next"
