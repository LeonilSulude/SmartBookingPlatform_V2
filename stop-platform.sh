#!/bin/bash

echo "Stopping SmartBookingPlatform..."

echo ""
echo "Stopping Spring Boot services..."

taskkill //F //IM java.exe > /dev/null 2>&1

echo "Java services stopped"

echo ""
echo "Stopping Docker infrastructure..."

docker compose down

echo ""
echo "Platform stopped"