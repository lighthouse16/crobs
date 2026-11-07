@echo off
setlocal
cd /d "%~dp0"
echo ==============================================
echo  Running CROBS Test Suite
echo ==============================================

java -cp "out;lib/*" TestRunner
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Test execution failed.
    exit /b %ERRORLEVEL%
) else (
    exit /b 0
)
