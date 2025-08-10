@echo off
echo ========================================
echo Starting Debate Arena Local Build
echo ========================================

echo 1. Running Gradle build...
call gradlew.bat clean build -x test

if %ERRORLEVEL% NEQ 0 (
    echo Build failed!
    pause
    exit /b 1
)

echo 2. Build completed! JAR file generated
echo 3. Checking JAR file...
if exist "build\libs\*.jar" (
    for %%f in (build\libs\*.jar) do (
        echo Found JAR file: %%f
    )
) else (
    echo No JAR file found in build\libs\
    pause
    exit /b 1
)

echo 4. Docker build ready
echo ========================================
echo Run the following command to build Docker image:
echo docker build -t debate-arena .
echo ========================================
pause
