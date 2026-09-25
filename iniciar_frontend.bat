@echo off
chcp 65001 >nul
title Visor Vial - Frontend (Angular 22)
color 0B

echo ============================================
echo   VISOR VIAL - FRONTEND Angular 22
echo ============================================
echo.

:: -----------------------------------------------
:: 1. Verificar Node.js
:: -----------------------------------------------
echo [1/3] Verificando Node.js...

node --version >nul 2>&1
if errorlevel 1 goto :node_no_encontrado

for /f %%v in ('node --version') do set NODE_VERSION=%%v
set NODE_VERSION_NUM=%NODE_VERSION:v=%
for /f "delims=." %%a in ("%NODE_VERSION_NUM%") do set NODE_MAJOR=%%a

echo  Node.js detectado: %NODE_VERSION%

if %NODE_MAJOR% LSS 22 goto :node_incompatible

echo  [OK] Compatible con Angular 22.
echo.
goto :verificar_deps

:node_no_encontrado
echo  Node.js no detectado. Instalando Node.js 24 LTS...
call :instalar_node
exit /b

:node_incompatible
echo  Node.js %NODE_VERSION% no es compatible con Angular 22.
echo  Instalando Node.js 24 LTS automaticamente...
call :instalar_node
exit /b

:: -----------------------------------------------
:: 2. Instalar dependencias si faltan
:: -----------------------------------------------
:verificar_deps
echo [2/3] Verificando dependencias...

set FRONTEND_DIR=%~dp0visor_vial\frontend

if not exist "%FRONTEND_DIR%\package.json" (
    echo.
    echo  [AVISO] No se encontro package.json en:
    echo          %FRONTEND_DIR%
    echo.
    echo  Asegurate de que este bat este en la carpeta raiz del proyecto
    echo  junto a la carpeta visor_vial\.
    echo.
    pause
    exit /b
)

if not exist "%FRONTEND_DIR%\node_modules\@angular\core" (
    echo  Instalando dependencias, primera vez tarda 2-3 minutos...
    echo.
    cd /d "%FRONTEND_DIR%"
    npm install
    if errorlevel 1 (
        echo.
        echo  [AVISO] No se pudieron instalar las dependencias.
        echo.
        echo  Posibles causas:
        echo    - Sin conexion a internet
        echo    - Permisos insuficientes: intenta ejecutar como Administrador
        echo.
        pause
        exit /b
    )
    echo  [OK] Dependencias instaladas.
) else (
    echo  [OK] Dependencias ya instaladas.
)
echo.

:: -----------------------------------------------
:: 3. Liberar puerto 4200 si esta en uso
:: -----------------------------------------------
echo [3/3] Iniciando Angular en http://localhost:4200 ...

set FRONT_PID=
for /f "tokens=5" %%p in ('netstat -ano 2^>nul ^| findstr ":4200 "') do (
    if not defined FRONT_PID set FRONT_PID=%%p
)
if defined FRONT_PID (
    echo  El puerto 4200 estaba en uso. Liberandolo...
    taskkill /PID %FRONT_PID% /F >nul 2>&1
    timeout /t 2 /nobreak >nul
    echo  [OK] Puerto 4200 liberado.
)

echo.
echo ============================================
echo  Navegador abrira automaticamente.
echo  Ctrl+C para detener.
echo ============================================
echo.

cd /d "%FRONTEND_DIR%"

start /b cmd /c "timeout /t 12 /nobreak >nul && start http://localhost:4200"

node_modules\.bin\ng.cmd serve
set NG_EXIT=%ERRORLEVEL%

if %NG_EXIT% NEQ 0 (
    echo.
    echo ============================================
    echo  El servidor se detuvo. Analizando causa...
    echo ============================================
    echo.

    :: Revisar si el puerto sigue ocupado
    netstat -ano 2>nul | findstr ":4200 " >nul
    if not errorlevel 1 (
        echo  [AVISO] Puerto 4200 en uso.
        echo          Cierra la aplicacion que lo ocupa y vuelve a ejecutar.
        goto :fin_front
    )

    :: Revisar si node_modules esta bien
    if not exist "%FRONTEND_DIR%\node_modules\@angular\core" (
        echo  [AVISO] Las dependencias parecen estar incompletas o corruptas.
        echo          Elimina la carpeta node_modules y vuelve a ejecutar el bat.
        goto :fin_front
    )

    echo  [AVISO] Angular termino inesperadamente.
    echo          Revisa los mensajes de arriba para mas detalles.
)

:fin_front
echo.
pause
exit /b

:: -----------------------------------------------
:: Subrutina: instalar Node 24 via winget
:: -----------------------------------------------
:instalar_node
winget --version >nul 2>&1
if errorlevel 1 (
    echo.
    echo  [AVISO] winget no esta disponible en este sistema.
    echo          Instala Node.js 24 LTS manualmente desde:
    echo          https://nodejs.org/
    echo.
    pause
    exit /b
)
echo.
echo  Instalando Node.js 24 LTS via winget...
echo  (Puede tardar unos minutos)
echo.
winget install OpenJS.NodeJS.LTS --silent --accept-package-agreements --accept-source-agreements
if errorlevel 1 (
    echo.
    echo  [AVISO] No se pudo instalar Node.js automaticamente.
    echo          Instala Node.js 24 LTS manualmente desde: https://nodejs.org/
    echo.
    pause
    exit /b
)
echo.
echo  [OK] Node.js 24 LTS instalado correctamente.
echo.
echo  Cierra esta ventana y vuelve a ejecutar el bat.
echo  (Windows necesita recargar el PATH para reconocer Node.js)
echo.
pause
exit /b
