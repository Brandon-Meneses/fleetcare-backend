package com.tuorg.fleetcare.maintanance

import com.fasterxml.jackson.annotation.JsonCreator
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "maintenance_orders")
data class MaintenanceOrder(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    val id: String? = null,

    @Column(nullable = false)
    val busId: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: MaintenanceType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: MaintenanceStatus = MaintenanceStatus.PLANNED,

    val plannedAt: LocalDateTime? = null,
    val openedAt: LocalDateTime? = null,
    val closedAt: LocalDateTime? = null,
    val notes: String? = null,
)

enum class MaintenanceType {
    PREVENTIVE,
    CORRECTIVE;

    @JsonCreator
    fun fromValue(value: String): MaintenanceType =
        entries.first { it.name.equals(value, ignoreCase = true) }
}
enum class MaintenanceStatus { PLANNED, OPEN, CLOSED }