#!/usr/bin/env bash
set -e

echo "=============================================="
echo " Compiling CROBS"
echo "=============================================="

mkdir -p out

javac -encoding UTF-8 -cp "lib/*:src:test" -d out \
  $(find src test -name "*.java")

echo "[SUCCESS] Compilation completed successfully."
