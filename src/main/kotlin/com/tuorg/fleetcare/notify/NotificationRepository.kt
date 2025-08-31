package com.tuorg.fleetcare.notify

import org.springframework.data.domain.Page
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.domain.Pageable

interface NotificationRepository : JpaRepository<Notification, Long> {
    fun findByUserEmailAndReadIsFalseOrderByCreatedAtDesc(email: String): List<Notification>

    fun findByUserEmailAndReadIsFalse(email: String, pageable: Pageable): Page<Notification>

    fun countByUserEmailAndReadIsFalse(email: String): Long

}