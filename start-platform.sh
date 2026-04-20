#!/bin/bash

echo "Starting SmartBookingPlatform..."

# Load JWT secret if not defined
if [ -z "$JWT_SECRET" ]; then
  export JWT_SECRET="uma-super-secret-key-com-pelo-menos-32-caracteres"
  echo "JWT secret loaded"
fi

echo ""
echo "Starting infrastructure (Docker)..."
docker compose up -d

echo ""
echo "Waiting for infrastructure..."
sleep 5


start_service() {

  name=$1
  path=$2

  echo "Starting $name..."

  (
    cd "$path"
    ./mvnw spring-boot:run > "$name.log" 2>&1
  ) &
}

wait_for_port() {

  port=$1
  name=$2

  echo "Checking $name (port $port)..."

  for i in {1..60}; do
    if netstat -ano | grep ":$port" | grep LISTENING > /dev/null; then
      echo "✓ $name ready"
      return
    fi
    sleep 1
  done

  echo "✗ $name failed to start"
}


echo ""
echo "Starting microservices (async)..."

start_service "discovery-service" "discovery-service"
start_service "auth-service" "auth-service"
start_service "catalog-service" "catalog-service"
start_service "booking-service" "booking-service"
start_service "api-gateway" "api-gateway"
start_service "log-service" "log-service"


echo ""
echo "Waiting for services..."

wait_for_port 8761 "Discovery Service"
wait_for_port 8081 "Auth Service"
wait_for_port 8082 "Catalog Service"
wait_for_port 8083 "Booking Service"
wait_for_port 8080 "API Gateway"
wait_for_port 8090 "Log Service"


echo ""
echo "Platform started successfully"
echo ""
echo "Services:"
echo "Eureka:      http://localhost:8761"
echo "Gateway:     http://localhost:8080"
echo "Log Service:  http://localhost:8090/actuator/health"