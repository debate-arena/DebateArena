#!/bin/bash

echo "========================================"
echo "Starting Debate Service Local Build"
echo "======================================="

echo "1. Running Gradle build..."
./gradlew clean build -x test

if [ $? -ne 0 ]; then
    echo "Build failed!"
    exit 1
fi

echo "2. Build completed! JAR file generated"
echo "3. Checking JAR file..."
if ls build/libs/*.jar 1> /dev/null 2>&1; then
    echo "[SUCCESS] Found JAR files:"
    ls -la build/libs/*.jar
else
    echo "[ERROR] No JAR file found in build/libs/"
    echo "Current directory: $(pwd)"
    echo "Contents of build/libs/:"
    if [ -d "build/libs" ]; then
        ls -la build/libs/
    else
        echo "build/libs directory does not exist"
    fi
    exit 1
fi

echo "4. Docker build ready"
echo "======================================="
