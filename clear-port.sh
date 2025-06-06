#!/bin/bash

# Function to check if a port is in use
is_port_in_use() {
    local port=$1
    lsof -i :$port -sTCP:LISTEN -t
}

# Function to stop process running on a port
stop_process_on_port() {
    local port=$1
    local pid=$(is_port_in_use $port)
    if [ -n "$pid" ]; then
        echo "Stopping process running on port $port"
        kill -9 $pid
        while lsof -i :$port -sTCP:LISTEN -t >/dev/null; do
            sleep 1
        done
    fi
}


# Stop if ports are in use
stop_process_on_port 8761  # Eureka Server default port
stop_process_on_port 8081  # Employee application default port
stop_process_on_port 8082  # Product application default port
stop_process_on_port 8080  # API gateway default port

# Clear
Clear
