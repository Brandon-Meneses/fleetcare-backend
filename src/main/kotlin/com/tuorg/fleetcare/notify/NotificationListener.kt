package com.tuorg.fleetcare.notify

import com.tuorg.fleetcare.service.EmailService
import jakarta.persistence.PostPersist
import org.springframework.stereotype.Component

@Component
class NotificationListener(
    private val email: EmailService
) {
    @PostPersist
    fun afterSave(n: Notification) {
        email.send(n.userEmail, n.title, n.content)
    }
}