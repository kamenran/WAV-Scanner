@echo off
setlocal

set "APP_DIR=%~dp0"
set "JAR_PATH=%APP_DIR%dist\WAV-Scanner.jar"

where java >nul 2>nul
if errorlevel 1 (
  echo Java is required to run WAV Scanner.
  echo Install Java 17 or newer, then try again.
  pause
  exit /b 1
)

if not exist "%JAR_PATH%" (
  if exist "%APP_DIR%scripts\build-windows.bat" (
    call "%APP_DIR%scripts\build-windows.bat"
    if errorlevel 1 (
      pause
      exit /b 1
    )
  ) else (
    echo Could not find dist\WAV-Scanner.jar or scripts\build-windows.bat.
    pause
    exit /b 1
  )
)

cd /d "%APP_DIR%"
java -jar "%JAR_PATH%"
if errorlevel 1 pause
