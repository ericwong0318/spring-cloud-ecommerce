package com.example.payment.mapper;

import com.example.common.dto.PaymentDto;
import com.example.payment.model.Payment;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    PaymentMapper INSTANCE = Mappers.getMapper(PaymentMapper.class);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "orderId", source = "orderId")
    @Mapping(target = "amount", source = "amount")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "gatewayTransactionId", source = "gatewayTransactionId")
    @Mapping(target = "idempotencyKey", source = "idempotencyKey")
    @Mapping(target = "authorizedAt", source = "authorizedAt")
    @Mapping(target = "capturedAt", source = "capturedAt")
    @Mapping(target = "refundedAt", source = "refundedAt")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    PaymentDto toDto(Payment payment);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "orderId", source = "orderId")
    @Mapping(target = "amount", source = "amount")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "gatewayTransactionId", source = "gatewayTransactionId")
    @Mapping(target = "idempotencyKey", source = "idempotencyKey")
    Payment toEntity(PaymentDto paymentDto);
}