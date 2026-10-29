@echo off
setlocal
echo ==============================================
echo  Starting CROBS Desktop GUI Application
echo ==============================================

if "%~1"=="" (
    java -cp "out;lib/*" hk.edu.polyu.comp.comp2021.crobs.gui.CrobsGuiApplication -html gui_session.html -txt gui_session.txt
) else (
    java -cp "out;lib/*" hk.edu.polyu.comp.comp2021.crobs.gui.CrobsGuiApplication %*
)
