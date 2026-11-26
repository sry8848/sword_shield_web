@echo off
setlocal
chcp 65001 >nul

rem 1. 允许 JDBC 在首次连接时创建本地演示数据库。
set "SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/sword_shield?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&createDatabaseIfNotExist=true"

rem 2. 从 NVM 目录选用本机安装的 Node 24。
rem Vite 无法在本机默认的 Node 16 上启动，临时调整 PATH 可保留系统默认版本。
if defined NVM_HOME if exist "%NVM_HOME%\v24.18.0\node.exe" set "PATH=%NVM_HOME%\v24.18.0;%PATH%"

rem 3. 使用增强版已有的脚本启动数据库、后端和前端。
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0sword_shield_web\scripts\start-local.ps1"
set "start_status=%errorlevel%"

rem 4. 保留窗口显示启动结果，并返回 PowerShell 的退出码。
pause
exit /b %start_status%
