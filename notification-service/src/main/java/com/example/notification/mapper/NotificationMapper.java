package com.example.notification.mapper;

import com.example.common.dto.NotificationDto;
import com.example.notification.model.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationMapper INSTANCE = Mappers.getMapper(NotificationMapper.class);

    @Mapping(source = "type", target = "type", qualifiedByName = "mapTypeToDto")
    @Mapping(source = "channel", target = "channel", qualifiedByName = "mapChannelToDto")
    @Mapping(source = "status", target = "status", qualifiedByName = "mapStatusToDto")
    @Mapping(source = "retryCount", target = "retryCount")
    @Mapping(source = "maxRetries", target = "maxRetries")
    @Mapping(source = "fallbackChannel", target = "fallbackChannel")
    NotificationDto toDto(Notification notification);

    @Mapping(source = "type", target = "type", qualifiedByName = "mapTypeToEntity")
    @Mapping(source = "channel", target = "channel", qualifiedByName = "mapChannelToEntity")
    @Mapping(source = "status", target = "status", qualifiedByName = "mapStatusToEntity")
    @Mapping(source = "retryCount", target = "retryCount")
    @Mapping(source = "maxRetries", target = "maxRetries")
    @Mapping(source = "fallbackChannel", target = "fallbackChannel")
    Notification toEntity(NotificationDto notificationDto);

    @Named("mapTypeToDto")
    default NotificationDto.NotificationType mapTypeToDto(Notification.NotificationType type) {
        if (type == null) return null;
        return NotificationDto.NotificationType.valueOf(type.name());
    }

    @Named("mapChannelToDto")
    default NotificationDto.NotificationChannel mapChannelToDto(Notification.NotificationChannel channel) {
        if (channel == null) return null;
        return NotificationDto.NotificationChannel.valueOf(channel.name());
    }

    @Named("mapStatusToDto")
    default NotificationDto.NotificationStatus mapStatusToDto(Notification.NotificationStatus status) {
        if (status == null) return null;
        return NotificationDto.NotificationStatus.valueOf(status.name());
    }

    @Named("mapTypeToEntity")
    default Notification.NotificationType mapTypeToEntity(NotificationDto.NotificationType type) {
        if (type == null) return null;
        return Notification.NotificationType.valueOf(type.name());
    }

    @Named("mapChannelToEntity")
    default Notification.NotificationChannel mapChannelToEntity(NotificationDto.NotificationChannel channel) {
        if (channel == null) return null;
        return Notification.NotificationChannel.valueOf(channel.name());
    }

    @Named("mapStatusToEntity")
    default Notification.NotificationStatus mapStatusToEntity(NotificationDto.NotificationStatus status) {
        if (status == null) return null;
        return Notification.NotificationStatus.valueOf(status.name());
    }
}
