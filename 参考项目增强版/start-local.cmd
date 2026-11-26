@echo off
setlocal

rem 1. Let JDBC create the local demo database on first use.
set "SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/sword_shield?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&createDatabaseIfNotExist=true"

rem 2. Prefer the installed Node 24 for this launch.
rem Vite needs a newer Node version than the system default Node 16.
if defined NVM_HOME if exist "%NVM_HOME%\v24.18.0\node.exe" set "PATH=%NVM_HOME%\v24.18.0;%PATH%"

rem 3. Use the existing PowerShell launcher for MySQL, backend, and frontend.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0sword_shield_web\scripts\start-local.ps1"
set "start_status=%errorlevel%"

rem 4. Keep the result visible and return the PowerShell exit code.
pause
exit /b %start_status%
