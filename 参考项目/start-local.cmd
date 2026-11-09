@echo off
chcp 65001 >nul
cd /d "%~dp0"

rem 1. 启动 Docker 依赖，失败时保留窗口显示错误。
docker compose up -d --pull never
if errorlevel 1 (
    pause
    exit /b 1
)

rem 2. 各后端分别打开终端，直接显示 Maven 和应用输出。
for %%m in (user activity content interaction file admin gateway) do (
    start "sword-shield-%%m" /D "%~dp0sword-shield-%%m" cmd /k "mvn.cmd spring-boot:run"
)

rem 3. 启动前端；即使命令失败，终端也会保留报错。
start "sword-shield-web-ui" /D "%~dp0sword-shield-web-ui" cmd /k "npm.cmd run dev"

echo 前端就绪后访问：http://localhost:5173/
echo 后台登录地址：http://localhost:5173/admin/login
echo 各服务的输出和报错显示在对应终端中。
pause
