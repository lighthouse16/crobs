@echo off
setlocal
cd /d "%~dp0"
echo ==============================================
echo  Starting CROBS CLI Application
echo ==============================================

if "%~1"=="" (
    java -cp "out;lib/*" hk.edu.polyu.comp.comp2021.crobs.Application -html cli_session.html -txt cli_session.txt
) else (
    java -cp "out;lib/*" hk.edu.polyu.comp.comp2021.crobs.Application %*
)
