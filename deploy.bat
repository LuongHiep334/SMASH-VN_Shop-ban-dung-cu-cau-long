@echo off
chcp 65001 >nul
color 0A
title Universal Deploy Tool v2.0

cd /d "%~dp0"
set GIT_PAGER=cat

::===========================================
:: KIEM TRA GIT
::===========================================

git rev-parse --is-inside-work-tree >nul 2>&1

if errorlevel 1 (
    color 0C
    echo.
    echo ==========================================
    echo    Day khong phai Git Repository!
    echo ==========================================
    echo.
    pause
    exit
)

::===========================================
:: MENU
::===========================================

:MENU

cls

echo.
echo ===============================================
echo          UNIVERSAL DEPLOY TOOL v2.0
echo ===============================================
echo.

echo Project:
echo %CD%
echo.

git branch --show-current

echo.
echo -----------------------------------------------
echo.

if exist pom.xml (
echo [1] Build Maven Project
)

if exist deploy.js (
echo [2] Deploy (deploy.js)
)

echo [3] Git Add + Commit + Push
echo [4] Git Pull (Xem Commit ^& Nguoi Sua)
echo [5] Git Status
echo [6] Build + Deploy + Push
echo [0] Exit

echo.
set /p choice=Chon:

if "%choice%"=="1" goto BUILD
if "%choice%"=="2" goto DEPLOY
if "%choice%"=="3" goto GIT
if "%choice%"=="4" goto PULL
if "%choice%"=="5" goto STATUS
if "%choice%"=="6" goto ALL
if "%choice%"=="0" exit

goto MENU

::===========================================
:: BUILD
::===========================================

:BUILD

cls

if not exist pom.xml (
echo.
echo Khong phai Maven Project.
pause
goto MENU
)

echo.
echo ===== BUILD =====
echo.

if exist mvnw.cmd (
call mvnw.cmd clean package -DskipTests
) else (
call mvn clean package -DskipTests
)

if errorlevel 1 (
color 0C
echo.
echo BUILD FAILED
pause
goto MENU
)

echo.
echo BUILD SUCCESS

pause
goto MENU

::===========================================
:: DEPLOY
::===========================================

:DEPLOY

cls

if not exist deploy.js (
echo.
echo Khong tim thay deploy.js
pause
goto MENU
)

echo.
echo ===== DEPLOY =====
echo.

node deploy.js

if errorlevel 1 (
color 0C
echo.
echo DEPLOY FAILED
pause
goto MENU
)

echo.
echo DEPLOY SUCCESS

pause
goto MENU

::===========================================
:: GIT
::===========================================

:GIT

cls

echo.
echo ===== GIT =====
echo.

git add .

echo.

set /p msg=Commit message:

if "%msg%"=="" (
for /f %%i in ('powershell -NoProfile -Command "Get-Date -Format yyyy-MM-dd_HH:mm:ss"') do set msg=Update_%%i
)

git commit -m "%msg%"

echo.

git push

if errorlevel 1 (
color 0C
echo.
echo PUSH FAILED
pause
goto MENU
)

echo.
echo PUSH SUCCESS

pause
goto MENU

::===========================================
:: PULL
::===========================================

:PULL

cls

echo.
echo ===============================================
echo        GIT PULL - KIEM TRA ^& CAP NHAT CODE
echo ===============================================
echo.

echo [1/3] Dang kiem tra du lieu moi tu Server (git fetch)...
git fetch

echo.
echo ===============================================
echo [2/3] CAC COMMIT MOI TREN REMOTE (NEU CO):
echo ===============================================
git --no-pager log HEAD..@{u} --stat -n 5 2>nul

echo.
echo ===============================================
echo [3/3] DANG TIEN HANH GIT PULL...
echo ===============================================
git pull

echo.
echo ===============================================
echo LICH SU 5 COMMIT MOI NHAT (NGUOI SUA ^& FILE THAY DOI):
echo ===============================================
git --no-pager log -n 5 --stat

echo.
pause
goto MENU

::===========================================
:: STATUS
::===========================================

:STATUS

cls

git --no-pager status

pause
goto MENU

::===========================================
:: ALL
::===========================================

:ALL

if exist pom.xml (

if exist mvnw.cmd (
call mvnw.cmd clean package -DskipTests
) else (
call mvn clean package -DskipTests
)

if errorlevel 1 (
echo.
echo BUILD FAILED
pause
goto MENU
)

)

if exist deploy.js (

node deploy.js

if errorlevel 1 (
echo.
echo DEPLOY FAILED
pause
goto MENU
)

)

git add .

echo.

set /p msg=Commit message:

if "%msg%"=="" (
for /f %%i in ('powershell -NoProfile -Command "Get-Date -Format yyyy-MM-dd_HH:mm:ss"') do set msg=Update_%%i
)

git commit -m "%msg%"

git push

echo.
echo ======================================
echo       ALL TASK COMPLETED
echo ======================================

pause
goto MENU