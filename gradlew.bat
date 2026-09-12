@echo off
setlocal
set "GRADLE_VERSION=9.7.0"
set "DIST_DIR=%USERPROFILE%\.gradle\wrapper\dists\gradle-%GRADLE_VERSION%-bin"
set "INSTALL=%DIST_DIR%\gradle-%GRADLE_VERSION%"
if exist "%INSTALL%\bin\gradle.bat" goto run
if not exist "%DIST_DIR%" mkdir "%DIST_DIR%"
echo Gradle %GRADLE_VERSION% not found. Downloading...
powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%DIST_DIR%\gradle.zip'"
powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Force '%DIST_DIR%\gradle.zip' '%DIST_DIR%'"
del /q "%DIST_DIR%\gradle.zip"
:run
call "%INSTALL%\bin\gradle.bat" %*
endlocal
