package com.example.notification.mapper;

import com.example.common.dto.NotificationDto;
import com.example.notification.model.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationMapper INSTANCE = Mappers.getMapper(NotificationMapper.class);

    @Mapping(source = "type", target = "type")
    @Mapping(source = "status", target = "status")
    NotificationDto toDto(Notification notification);

    @Mapping(source = "type", target = "type")
    @Mapping(source = "status", target = "status")
    Notification toEntity(NotificationDto notificationDto);
}