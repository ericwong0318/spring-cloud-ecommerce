#!/bin/bash

 Start Eureka Server
cd ~/spring-cloud-project/eureka-server
./mvnw spring-boot:run &

# Start Product application
cd ~/spring-cloud-project/product
./mvnw spring-boot:run &

## Start API gateway
#cd ~/spring-cloud-project/gateway/
#./mvnw spring-boot:run &

