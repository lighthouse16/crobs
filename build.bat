@echo off
setlocal
echo ==============================================
echo  Compiling CROBS
echo ==============================================

if not exist out mkdir out

javac -encoding UTF-8 -cp "lib/*;src;test" -d out src/hk/edu/polyu/comp/comp2021/crobs/*.java src/hk/edu/polyu/comp/comp2021/crobs/model/*.java src/hk/edu/polyu/comp/comp2021/crobs/gui/*.java src/hk/edu/polyu/comp/comp2021/crobs/gui/dialog/*.java src/hk/edu/polyu/comp/comp2021/crobs/gui/view/*.java test/TestRunner.java test/hk/edu/polyu/comp/comp2021/crobs/model/*.java test/hk/edu/polyu/comp/comp2021/crobs/gui/*.java

if %ERRORLEVEL% equ 0 (
    echo [SUCCESS] Compilation completed successfully.
) else (
    echo [ERROR] Compilation failed.
    exit /b %ERRORLEVEL%
)
