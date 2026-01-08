#!/bin/bash
set -e

echo "Pulling latest changes..."
git fetch origin main
git checkout main
git reset --hard origin/main

echo "Building project..."
./gradlew clean build -x test

echo "Stopping old service..."
sudo systemctl stop proj || true

echo "Copying new JAR..."
cp build/libs/app.jar /home/ec2-user/app.jar

echo "Starting service..."
sudo systemctl start proj

echo "Deployment complete!"
