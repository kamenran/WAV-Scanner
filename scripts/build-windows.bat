@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
set "ROOT_DIR=%SCRIPT_DIR%.."
set "BUILD_DIR=%ROOT_DIR%\build"
set "CLASSES_DIR=%BUILD_DIR%\classes"
set "DIST_DIR=%ROOT_DIR%\dist"

if exist "%BUILD_DIR%" rmdir /s /q "%BUILD_DIR%"
if exist "%DIST_DIR%" rmdir /s /q "%DIST_DIR%"
mkdir "%CLASSES_DIR%"
mkdir "%DIST_DIR%"

javac -cp "%ROOT_DIR%\libs\*" -d "%CLASSES_DIR%" "%ROOT_DIR%\src\WAVKeyDetector.java"
if errorlevel 1 exit /b 1

for %%J in ("%ROOT_DIR%\libs\*.jar") do (
  pushd "%CLASSES_DIR%"
  jar xf "%%J"
  popd
)

del /s /q "%CLASSES_DIR%\module-info.class" >nul 2>nul

jar --create --file "%DIST_DIR%\WAV-Scanner.jar" --main-class WAVKeyDetector -C "%CLASSES_DIR%" .
if errorlevel 1 exit /b 1

(
  echo @echo off
  echo setlocal
  echo.
  echo where java ^>nul 2^>nul
  echo if errorlevel 1 ^(
  echo   echo Java is required to run WAV Scanner.
  echo   echo Install Java 17 or newer, then try again.
  echo   pause
  echo   exit /b 1
  echo ^)
  echo.
  echo cd /d "%%~dp0"
  echo java -jar "WAV-Scanner.jar"
  echo if errorlevel 1 pause
) > "%DIST_DIR%\Launch WAV Scanner.bat"

echo Built dist\WAV-Scanner.jar
echo Double-click dist\Launch WAV Scanner.bat on any computer with Java installed.
echo.
echo To build a Windows .exe, run scripts\build-windows-exe.bat on Windows with JDK 17+ installed.
