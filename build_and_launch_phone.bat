@echo off
echo =================================================================
echo   MAYA AI MARK-XXXIX - AUTOMATED COMPILATION & PHONE INSTALLER
echo =================================================================

set "JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.20.8-hotspot"
set "PATH=%JAVA_HOME%\bin;e:\wafa-ai\platform-tools;%PATH%"
set "GRADLE_BAT=e:\wafa-ai\gradle-dist\gradle-8.11.1\bin\gradle.bat"
set "ADB=e:\wafa-ai\platform-tools\adb.exe"

echo [1/3] Checking Connected Android Device...
"%ADB%" devices -l

echo [2/3] Compiling Maya AI APK (assembleDebug)...
call "%GRADLE_BAT%" assembleDebug --stacktrace

if not exist "e:\wafa-ai\app\build\outputs\apk\debug\app-debug.apk" (
    echo [ERROR] APK compilation failed or output not found.
    pause
    exit /b 1
)

echo [3/3] Installing Maya AI on Connected Phone...
"%ADB%" install -r -d "e:\wafa-ai\app\build\outputs\apk\debug\app-debug.apk"

echo [4/4] Launching Maya AI on Phone Screen...
"%ADB%" shell am start -n com.aistudio.firdousai.vzcyp/com.example.MainActivity

echo =================================================================
echo   SUCCESS! Maya AI Mark-XXXIX is now running on your phone!
echo =================================================================
pause
