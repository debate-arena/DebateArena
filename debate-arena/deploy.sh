#!/bin/bash

echo "========================================"
echo "Starting Debate Arena Full Deployment"
echo "========================================"

echo "1. Starting local build..."
./build.sh

if [ $? -ne 0 ]; then
    echo "Local build failed!"
    exit 1
fi

echo "2. Starting Docker build..."
docker build -t debate-arena .

if [ $? -ne 0 ]; then
    echo "Docker build failed!"
    exit 1
fi

echo "3. Stopping existing container..."
docker stop debate-arena 2>/dev/null || true
docker rm debate-arena 2>/dev/null || true

echo "4. Starting new container..."
docker run -d --name debate-arena -p 8080:8080 debate-arena

echo "========================================"
echo "Deployment completed! Check at http://localhost:8080"
echo "========================================"
