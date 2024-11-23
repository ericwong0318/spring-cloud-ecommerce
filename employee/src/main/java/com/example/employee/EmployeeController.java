package com.example.employee;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
public class EmployeeController {

    private final DiscoveryClient discoveryClient;
    private final RestClient restClient;

    public EmployeeController(DiscoveryClient discoveryClient, RestClient.Builder restClientBuilder) {
        this.discoveryClient = discoveryClient;
        restClient = restClientBuilder.build();
    }

    @GetMapping("/")
    public String helloWorld() {
        return "Employee service is running.";
    }

    @GetMapping("/employees/{id}")
    public Employee getEmployee(@PathVariable String id) {
        return new Employee(id, "John Doe", "Engineering");
    }


}