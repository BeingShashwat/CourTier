package com.courtier.courtier.notification.repository;

import com.courtier.courtier.notification.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByCnrNumberOrderByCreatedAtDesc(String cnrNumber);

    void deleteAllByUserId(Long userId);
}