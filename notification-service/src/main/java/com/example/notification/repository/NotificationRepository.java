package com.example.notification.repository;

import com.example.notification.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByStatus(Notification.NotificationStatus status);
    
    List<Notification> findByReferenceIdAndReferenceType(String referenceId, String referenceType);
    
    @Query("SELECT n FROM Notification n WHERE n.status = :status AND n.createdAt < :threshold")
    List<Notification> findStalePendingNotifications(@Param("status") Notification.NotificationStatus status, 
                                                      @Param("threshold") LocalDateTime threshold);
    
    @Query("SELECT n FROM Notification n WHERE n.status = :status AND n.retryCount < n.maxRetries")
    List<Notification> findRetryableNotifications(@Param("status") Notification.NotificationStatus status);
}