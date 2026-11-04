#!/usr/bin/env bash
# Renders the diorama art with Blender and turns it into app resources.
#   tools/art/render.sh            all facades (day and night), kinds and the icon
#   tools/art/render.sh facade 3 day [samples]   one job
# BLENDER points at blender.exe (default: the portable 4.2 LTS in %TEMP%\blender).
set -e
cd "$(dirname "$0")/../.."
B="${BLENDER:-$TEMP/blender/blender-4.2.3-windows-x64/blender.exe}"
run() { "$B" -b -P tools/art/diorama.py -- "$@" 2>&1 | grep -E "Error|Traceback|File \"|Saved" || true; }
if [ $# -gt 0 ]; then run "$@"; exit 0; fi
for st in 0 1 2 3 4 5 6 7; do for t in day night; do run facade $st $t; done; done
for k in cans figures magnets coins paper records cars own; do run kind $k; done
run icon
python tools/art/pack.py
