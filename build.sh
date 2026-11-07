#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=============================================="
echo " Compiling CROBS"
echo "=============================================="

mkdir -p out

javac -encoding UTF-8 -source 8 -target 8 -cp "lib/*:src:test" -d out \
  $(find src test -name "*.java")

echo "[SUCCESS] Compilation completed successfully."
