package com.tuorg.fleetcare.api.dto


import com.tuorg.fleetcare.maintanance.MaintenanceStatus
import com.tuorg.fleetcare.maintanance.MaintenanceType
import java.time.LocalDateTime

data class MaintenanceOrderRequest(
    val busId: String,
    val type: MaintenanceType,
    val plannedAt: LocalDateTime? = null,
    val notes: String? = null
)

data class MaintenanceOrderResponse(
    val id: String,
    val busId: String,
    val type: MaintenanceType,
    val status: MaintenanceStatus,
    val plannedAt: LocalDateTime?,
    val openedAt: LocalDateTime?,
    val closedAt: LocalDateTime?,
    val notes: String?
)