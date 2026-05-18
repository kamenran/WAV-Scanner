@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
set "ROOT_DIR=%SCRIPT_DIR%.."
set "DIST_DIR=%ROOT_DIR%\dist"
set "APP_DIR=%DIST_DIR%\exe"

call "%SCRIPT_DIR%build-windows.bat"
if errorlevel 1 exit /b 1

if exist "%APP_DIR%" rmdir /s /q "%APP_DIR%"
mkdir "%APP_DIR%"

jpackage ^
  --type exe ^
  --name "WAV Scanner" ^
  --input "%DIST_DIR%" ^
  --main-jar "WAV-Scanner.jar" ^
  --main-class WAVKeyDetector ^
  --dest "%APP_DIR%" ^
  --win-shortcut ^
  --win-menu

if errorlevel 1 (
  echo.
  echo jpackage could not build the installer.
  echo Make sure you are running this on Windows with JDK 17+ and WiX Toolset installed.
  exit /b 1
)

echo Built Windows installer in dist\exe
