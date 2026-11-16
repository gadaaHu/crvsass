@echo off
title CRVS Question Gathering & Assessment Server
cd /d "%~dp0backend"

echo ======================================================================
echo   CRVS Question Gathering & Assessment System (v2.1.8)
echo ======================================================================
echo   Starting Node.js Backend Server on port 4000...
echo   SQLite Database: %~dp0data\qcafr.sqlite
echo ======================================================================

start "" /b node dist/server.js

timeout /t 3 /nobreak > nul

echo.
echo   [OK] Server started!
echo   Web Portal:     http://localhost:4000
echo   Download App:   http://localhost:4000/download.html
echo   Health Check:   http://localhost:4000/api/health
echo ======================================================================
start http://localhost:4000/download.html
pause
