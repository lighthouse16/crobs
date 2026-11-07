#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=============================================="
echo " Starting CROBS CLI Application"
echo "=============================================="

if [ $# -eq 0 ]; then
    java -cp "out:lib/*" hk.edu.polyu.comp.comp2021.crobs.Application -html cli_session.html -txt cli_session.txt
else
    java -cp "out:lib/*" hk.edu.polyu.comp.comp2021.crobs.Application "$@"
fi
