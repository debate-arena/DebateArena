@echo off
echo ========================================
echo Starting Debate Arena Full Deployment
echo ========================================

echo 1. Starting local build...
call build.bat

if %ERRORLEVEL% NEQ 0 (
    echo Local build failed!
    pause
    exit /b 1
)

echo 2. Starting Docker build...
docker build -t debate-arena .

if %ERRORLEVEL% NEQ 0 (
    echo Docker build failed!
    pause
    exit /b 1
)

echo 3. Stopping existing container...
docker stop debate-arena 2>nul
docker rm debate-arena 2>nul

echo 4. Starting new container...
docker run -d --name debate-arena -p 8080:8080 debate-arena

echo ========================================
echo Deployment completed! Check at http://localhost:8080
echo ========================================
pause
