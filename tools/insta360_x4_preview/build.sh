#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
OUT_DIR="$ROOT_DIR/tools/insta360_x4_preview/build"

mkdir -p "$OUT_DIR"

g++ "$ROOT_DIR/tools/insta360_x4_preview/insta360_x4_preview.cc" \
  -std=c++17 \
  -I"$ROOT_DIR/CameraSDK/include" \
  -L"$ROOT_DIR/CameraSDK/lib" \
  -lCameraSDK \
  -pthread \
  -Wl,-rpath,"$ROOT_DIR/CameraSDK/lib" \
  -o "$OUT_DIR/insta360_x4_preview"

echo "$OUT_DIR/insta360_x4_preview"
