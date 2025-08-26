#!/bin/bash

echo "========================================"
echo "Checking Build Results"
echo "======================================="

ALL_READY=true

echo "Checking Debate Arena..."
if ls debate-arena/debate-arena/build/libs/*.jar 1> /dev/null 2>&1; then
    echo "[OK] Debate Arena JAR found:"
    ls -la debate-arena/debate-arena/build/libs/*.jar
else
    echo "[ERROR] Debate Arena JAR not found!"
    ALL_READY=false
fi

echo
echo "Checking Matching..."
if ls matching/matching/build/libs/*.jar 1> /dev/null 2>&1; then
    echo "[OK] Matching JAR found:"
    ls -la matching/matching/build/libs/*.jar
else
    echo "[ERROR] Matching JAR not found!"
    ALL_READY=false
fi

echo
echo "Checking Debate..."
if ls debate/debate/build/libs/*.jar 1> /dev/null 2>&1; then
    echo "[OK] Debate JAR found:"
    ls -la debate/debate/build/libs/*.jar
else
    echo "[ERROR] Debate JAR not found!"
    ALL_READY=false
fi

echo
if [ "$ALL_READY" = true ]; then
    echo "========================================"
    echo "All JAR files are ready for Docker build!"
    echo "========================================"
else
    echo "========================================"
    echo "Some JAR files are missing!"
    echo "Please run ./build-all.sh first."
    echo "========================================"
fi
