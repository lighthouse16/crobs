#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=============================================="
echo " Starting CROBS Desktop GUI Application"
echo "=============================================="

if [ $# -eq 0 ]; then
    java -cp "out:lib/*" hk.edu.polyu.comp.comp2021.crobs.gui.CrobsGuiApplication -html gui_session.html -txt gui_session.txt
else
    java -cp "out:lib/*" hk.edu.polyu.comp.comp2021.crobs.gui.CrobsGuiApplication "$@"
fi
