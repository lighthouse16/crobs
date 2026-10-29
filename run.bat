@echo off
chcp 65001 >nul
cd /d "%~dp0"
if not exist out mkdir out
echo [1/3] Compiling...
powershell -NoProfile -Command "javac -encoding UTF-8 -cp 'lib/*;src;test' -d out (Get-ChildItem -Recurse 'src\*.java','test\*.java' | Select-Object -ExpandProperty FullName)"
if %errorlevel% neq 0 (
    echo Compilation failed.
    pause
    exit /b 1
)

echo [2/3] Running Tests...
java -cp "out;lib/*" TestRunner
if %errorlevel% neq 0 (
    echo Tests failed.
    pause
    exit /b 1
)

echo [3/3] Launching CROBS GUI...
start javaw -cp "out;lib/*" hk.edu.polyu.comp.comp2021.crobs.gui.CrobsGuiApplication
echo Done.
