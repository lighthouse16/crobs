#!/usr/bin/env bash
set -e

echo "=============================================="
echo " Starting CROBS Desktop GUI Application"
echo "=============================================="

if [ $# -eq 0 ]; then
    java -cp "out:lib/*" hk.edu.polyu.comp.comp2021.crobs.gui.CrobsGuiApplication -html gui_session.html -txt gui_session.txt
else
    java -cp "out:lib/*" hk.edu.polyu.comp.comp2021.crobs.gui.CrobsGuiApplication "$@"
fi
