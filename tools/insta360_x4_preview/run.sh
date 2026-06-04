#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
BIN="$ROOT_DIR/tools/insta360_x4_preview/build/insta360_x4_preview"

if [[ ! -x "$BIN" ]]; then
  "$ROOT_DIR/tools/insta360_x4_preview/build.sh" >/dev/null
fi

export LD_LIBRARY_PATH="$ROOT_DIR/CameraSDK/lib:${LD_LIBRARY_PATH:-}"
exec "$BIN" "$@"
