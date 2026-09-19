package com.example.order;

import com.example.order.mapper.ShipmentMapper;
import com.example.order.model.Order;
import com.example.order.model.OrderItem;
import com.example.order.model.Shipment;
import com.example.order.model.ShipmentItem;
import com.example.common.event.OutboxEventPublisher;
import com.example.order.repository.OrderItemRepository;
import com.example.order.repository.OrderRepository;
import com.example.order.repository.ShipmentItemRepository;
import com.example.order.repository.ShipmentRepository;
import com.example.order.service.OrderService;
import com.example.order.service.ShipmentService;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;

/**
 * Minimal test configuration that only includes order-service beans, avoiding common module's JPA classes.
 */
@Configuration
@EnableAutoConfiguration(exclude = {
    org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
    org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.r2dbc.R2dbcRepositoriesAutoConfiguration.class
})
@ComponentScan(basePackages = "com.example.order", excludeFilters = {
    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        com.example.common.event.OutboxEventRepository.class,
        com.example.common.event.IdempotentEventProcessor.class,
        com.example.order.service.ShipmentService.class,
        com.example.order.service.OrderService.class
    })
})
public class MinimalTestConfig {
    @Bean
    public OutboxEventPublisher outboxEventPublisher() {
        return Mockito.mock(OutboxEventPublisher.class);
    }

    @Bean
    @Primary
    public ShipmentService shipmentService() {
        return Mockito.mock(ShipmentService.class);
    }

    @Bean
    @Primary
    public OrderService orderService() {
        return Mockito.mock(OrderService.class);
    }

    @Bean
    public OrderRepository orderRepository() {
        return Mockito.mock(OrderRepository.class);
    }

    @Bean
    public OrderItemRepository orderItemRepository() {
        return Mockito.mock(OrderItemRepository.class);
    }

    @Bean
    public ShipmentRepository shipmentRepository() {
        return Mockito.mock(ShipmentRepository.class);
    }

    @Bean
    public ShipmentItemRepository shipmentItemRepository() {
        return Mockito.mock(ShipmentItemRepository.class);
    }

    @Bean
    public ShipmentMapper shipmentMapper() {
        return Mockito.mock(ShipmentMapper.class);
    }
}