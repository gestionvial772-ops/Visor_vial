@echo off
chcp 65001 >nul
title Visor Vial - Backend (Spring Boot)
color 0A

:: Auto-elevar a administrador si no se tiene
net session >nul 2>&1
if errorlevel 1 (
    echo  Solicitando permisos de administrador...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
    exit /b
)

echo ============================================
echo   VISOR VIAL - BACKEND Spring Boot 4
echo ============================================
echo.

:: -----------------------------------------------
:: 1. Verificar Java 24
:: -----------------------------------------------
echo [1/4] Verificando Java...

set JAVA24_HOME=

:: Buscar en Eclipse Adoptium
if exist "C:\Program Files\Eclipse Adoptium\" (
    for /d %%d in ("C:\Program Files\Eclipse Adoptium\jdk-24.*") do (
        if not defined JAVA24_HOME set "JAVA24_HOME=%%d"
    )
)
:: Buscar en Java clasico
if not defined JAVA24_HOME (
    if exist "C:\Program Files\Java\" (
        for /d %%d in ("C:\Program Files\Java\jdk-24*") do (
            if not defined JAVA24_HOME set "JAVA24_HOME=%%d"
        )
    )
)
:: Buscar en Microsoft
if not defined JAVA24_HOME (
    if exist "C:\Program Files\Microsoft\" (
        for /d %%d in ("C:\Program Files\Microsoft\jdk-24*") do (
            if not defined JAVA24_HOME set "JAVA24_HOME=%%d"
        )
    )
)

if defined JAVA24_HOME (
    echo  [OK] Java 24 encontrado en: %JAVA24_HOME%
    set "JAVA_HOME=%JAVA24_HOME%"
    set "PATH=%JAVA24_HOME%\bin;%PATH%"
    goto :paso2
)

:: Java 24 no instalado - descargar e instalar automaticamente
echo  Java 24 no encontrado.
echo  Descargando e instalando Java 24 JDK automaticamente...
echo  (Puede tardar varios minutos segun tu conexion, por favor espera)
echo.

set JAVA_MSI=%TEMP%\temurin24.msi
set JAVA_URL=https://github.com/adoptium/temurin24-binaries/releases/download/jdk-24.0.1+9/OpenJDK24U-jdk_x64_windows_hotspot_24.0.1_9.msi

echo  Descargando desde Adoptium...
powershell -NoProfile -ExecutionPolicy Bypass -Command "& { [Net.ServicePointManager]::SecurityProtocol = 'Tls12'; (New-Object Net.WebClient).DownloadFile('%JAVA_URL%', '%JAVA_MSI%') }"

if not exist "%JAVA_MSI%" (
    echo.
    echo  [ERROR] No se pudo descargar Java 24.
    echo          Verifica tu conexion a internet o instala manualmente:
    echo          https://adoptium.net/
    echo.
    pause
    exit /b
)

echo  Instalando Java 24 JDK (requiere permisos de administrador)...
msiexec /i "%JAVA_MSI%" /quiet /norestart ADDLOCAL=FeatureMain,FeatureEnvironment,FeatureJarFileRunWith,FeatureJavaHome
if errorlevel 1 (
    echo.
    echo  [ERROR] La instalacion fallo.
    echo          Intenta ejecutar este bat como Administrador
    echo          (clic derecho sobre el bat - Ejecutar como administrador)
    echo.
    del "%JAVA_MSI%" >nul 2>&1
    pause
    exit /b
)

del "%JAVA_MSI%" >nul 2>&1
echo  [OK] Java 24 instalado correctamente.
echo.

:: Buscar la ruta recien instalada
for /d %%d in ("C:\Program Files\Eclipse Adoptium\jdk-24.*") do (
    if not defined JAVA24_HOME set "JAVA24_HOME=%%d"
)

if not defined JAVA24_HOME (
    echo  [AVISO] Ruta de Java 24 no detectada automaticamente.
    echo          Cierra esta ventana y vuelve a ejecutar el bat.
    echo.
    pause
    exit /b
)

set "JAVA_HOME=%JAVA24_HOME%"
set "PATH=%JAVA24_HOME%\bin;%PATH%"
echo  Usando: %JAVA24_HOME%
echo.

:: -----------------------------------------------
:: 2. Verificar Gradle Wrapper
:: -----------------------------------------------
:paso2
echo [2/4] Verificando Gradle Wrapper...

set "BACKEND_DIR=%~dp0Visor_vial\backend"

if not exist "%BACKEND_DIR%\gradlew.bat" (
    echo.
    echo  [AVISO] No se encontro gradlew.bat en:
    echo          %BACKEND_DIR%
    echo.
    echo  Asegurate de que este bat este en la carpeta raiz del proyecto.
    echo.
    pause
    exit /b
)
echo  [OK] Gradle Wrapper encontrado.
echo.

:: -----------------------------------------------
:: 3. Liberar puerto 8080 si esta en uso
:: -----------------------------------------------
echo [3/4] Verificando puerto 8080...

set PORT_PID=
for /f "tokens=5" %%p in ('netstat -ano 2^>nul ^| findstr ":8080 "') do (
    if not defined PORT_PID set PORT_PID=%%p
)

if defined PORT_PID (
    echo  Puerto 8080 en uso. Liberandolo automaticamente...
    taskkill /PID %PORT_PID% /F >nul 2>&1
    timeout /t 2 /nobreak >nul
    echo  [OK] Puerto 8080 liberado.
) else (
    echo  [OK] Puerto 8080 disponible.
)
echo.

:: -----------------------------------------------
:: 4. Iniciar el backend
:: -----------------------------------------------
echo [4/4] Iniciando Spring Boot en http://localhost:8080 ...
echo.
echo ============================================
echo  Logs del servidor aparecen abajo.
echo  Ctrl+C para detener.
echo ============================================
echo.

cd /d "%BACKEND_DIR%"
call gradlew.bat bootRun
set GRADLE_EXIT=%ERRORLEVEL%

if %GRADLE_EXIT% EQU 0 goto :fin
if %GRADLE_EXIT% EQU 1073741510 goto :fin

echo.
echo ============================================
echo  El servidor se detuvo con un error.
echo ============================================
echo.
echo  Revisa los logs de arriba para el error exacto.
echo  Causas comunes:
echo    - Error en el codigo Java
echo    - Primera ejecucion con descarga de dependencias interrumpida
echo      (vuelve a ejecutar este bat)

:fin
echo.
pause
exit /b
