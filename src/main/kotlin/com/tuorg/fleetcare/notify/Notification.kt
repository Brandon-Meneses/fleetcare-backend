package com.tuorg.fleetcare.notify

import jakarta.persistence.*
import java.time.Instant

@Entity
@EntityListeners(NotificationListener::class)
@Table(name = "notifications")
class Notification(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var userEmail: String,
    var title: String,
    var content: String,
    var link: String? = null,     // e.g. /report/history?area=OPERATIONS
    var read: Boolean = false,
    var createdAt: Instant = Instant.now()
)