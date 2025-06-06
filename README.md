# Java Spring Cloud

# API gateway

# Service discovery and Load Balancer

Client-Side Load Balancing

Eureka

# Service

| Feature                   | Client-Side Load Balancing                        | Server-Side Load Balancing                |
|---------------------------|---------------------------------------------------|-------------------------------------------|
| Decision Maker            | Client                                            | Dedicated Load Balancer                   |
| Implementation Example    | Spring Cloud LoadBalancer, Netflix Ribbon         | AWS ELB, NGINX, HAProxy                   |
| Latency                   | Lower latency (direct communication)              | Higher latency (additional network hop)    |
| Complexity for Clients    | More complex (clients manage instances)           | Simpler (clients only know load balancer)  |
| Single Point of Failure   | No                                                | Yes                                       |
| Cost                      | Cost-effective (no dedicated hardware)            | May require additional infrastructure      |