#!/usr/bin/env bash
set -e

echo "=============================================="
echo " Running CROBS Test Suite"
echo "=============================================="

java -cp "out:lib/*" TestRunner
