@echo off
REM ============================================================
REM  Lost & Found - Smart Campus Asset Recovery Platform
REM  Double-click this file to start the app.
REM  Needs Java 17 or newer installed on this computer.
REM ============================================================
title Lost and Found - Smart Campus Platform
cd /d "%~dp0"

where java >nul 2>nul
if %errorlevel% neq 0 (
    echo.
    echo  Java was not found on this computer.
    echo  Please install Java 17 or newer from https://adoptium.net
    echo  and then run this file again.
    echo.
    pause
    exit /b 1
)

echo.
echo  Starting Lost ^& Found...
echo  The app will open in your browser at http://localhost:8080
echo  Keep this window open while using the app. Close it to stop.
echo.
start "" http://localhost:8080
java -jar lost-and-found.jar
pause
