#!/bin/bash

echo "========================================"
echo "Starting Local Build for All Services"
echo "======================================="

echo "1. Building Debate Arena Service (bootJar)..."
cd debate-arena/debate-arena
sudo bash build-dev.sh
if [ $? -ne 0 ]; then
    echo "Debate Arena build failed!"
    cd ../..
    exit 1
fi
cd ../..

echo "2. Building Matching Service (bootJar)..."
cd matching/matching
sudo bash build-dev.sh
if [ $? -ne 0 ]; then
    echo "Matching build failed!"
    cd ../..
    exit 1
fi
cd ../..

echo "3. Building Debate Service (bootJar)..."
cd debate/debate
sudo bash build-dev.sh
if [ $? -ne 0 ]; then
    echo "Debate build failed!"
    cd ../..
    exit 1
fi
cd ../..

echo "========================================"
echo "All services build completed!"
echo "Checking build results..."
sudo bash check-build.sh

echo "========================================"
echo "If all JAR files are found, run:"
echo "docker-compose up --build"
echo "========================================"