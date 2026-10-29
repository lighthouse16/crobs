@echo off
setlocal
echo ==============================================
echo  Running CROBS Test Suite
echo ==============================================

java -cp "out;lib/*" TestRunner
