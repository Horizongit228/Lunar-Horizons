@echo off
setlocal
cd /d "%~dp0"
where gradle >nul 2>nul
if not errorlevel 1 (
  call gradle build
  exit /b
)
set "CACHE=%CD%\.gradle-local"
if not exist "%CACHE%" mkdir "%CACHE%"
if not exist "%CACHE%\gradle-8.8\bin\gradle.bat" (
  powershell -NoProfile -Command "$ErrorActionPreference='Stop'; Invoke-WebRequest 'https://services.gradle.org/distributions/gradle-8.8-bin.zip' -OutFile '%CACHE%\gradle.zip'; Expand-Archive -Path '%CACHE%\gradle.zip' -DestinationPath '%CACHE%' -Force"
  if errorlevel 1 exit /b 1
)
call "%CACHE%\gradle-8.8\bin\gradle.bat" build
exit /b %errorlevel%
