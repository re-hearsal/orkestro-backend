package io.github.Romariok.orkestro.notification.repository;

import io.github.Romariok.orkestro.notification.models.InAppNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InAppNotificationRepository extends JpaRepository<InAppNotification, Long> {

    Page<InAppNotification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
