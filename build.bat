@echo off
setlocal enabledelayedexpansion

set "JAR_NAME=sprint-mvc.jar"
set "DEST_DIR=..\Test-framework\lib"
set "JAR_PATH=target\sprint-mvc.jar"

echo [1/3] Clean...
call mvn clean
if errorlevel 1 (
    echo Erreur lors du clean
    exit /b 1
)

echo [2/3] Package...
call mvn package
if errorlevel 1 (
    echo Erreur lors du packaging
    exit /b 1
)

if not exist "%JAR_PATH%" (
    echo JAR introuvable : %JAR_PATH%
    exit /b 1
)

echo JAR trouve : %JAR_PATH%

echo [3/3] Copie vers Test-framework...

if not exist "%DEST_DIR%" (
    mkdir "%DEST_DIR%"
)

copy /Y "%JAR_PATH%" "%DEST_DIR%\%JAR_NAME%" >nul

if errorlevel 1 (
    echo Erreur de copie
    exit /b 1
) else (
    echo Copie reussie : %DEST_DIR%\%JAR_NAME%
)

endlocal
exit /b 0