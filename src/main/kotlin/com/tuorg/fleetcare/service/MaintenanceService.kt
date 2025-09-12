package com.tuorg.fleetcare.service

import com.tuorg.fleetcare.api.dto.MaintenanceOrderRequest
import com.tuorg.fleetcare.api.dto.MaintenanceOrderResponse
import com.tuorg.fleetcare.maintanance.MaintenanceOrder
import com.tuorg.fleetcare.maintanance.MaintenanceOrderRepository
import com.tuorg.fleetcare.maintanance.MaintenanceStatus
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class MaintenanceService(
    private val repo: MaintenanceOrderRepository
) {
    fun listAll(): List<MaintenanceOrderResponse> =
        repo.findAll().map { it.toResponse() }

    fun listByBus(busId: String): List<MaintenanceOrderResponse> =
        repo.findByBusId(busId).map { it.toResponse() }

    fun create(req: MaintenanceOrderRequest): MaintenanceOrderResponse {
        val entity = MaintenanceOrder(
            busId = req.busId,
            type = req.type,
            status = MaintenanceStatus.PLANNED,
            plannedAt = req.plannedAt,
            notes = req.notes
        )
        return repo.save(entity).toResponse()
    }

    fun open(id: String): MaintenanceOrderResponse {
        val order = repo.findById(id).orElseThrow()
        val updated = order.copy(
            status = MaintenanceStatus.OPEN,
            openedAt = LocalDateTime.now()
        )
        return repo.save(updated).toResponse()
    }

    fun close(id: String, notes: String?): MaintenanceOrderResponse {
        val order = repo.findById(id).orElseThrow()
        val updated = order.copy(
            status = MaintenanceStatus.CLOSED,
            closedAt = LocalDateTime.now(),
            notes = notes ?: order.notes
        )
        return repo.save(updated).toResponse()
    }
}

// 🔹 Extension mapper
fun MaintenanceOrder.toResponse() = MaintenanceOrderResponse(
    id = this.id!!,
    busId = this.busId,
    type = this.type,
    status = this.status,
    plannedAt = this.plannedAt,
    openedAt = this.openedAt,
    closedAt = this.closedAt,
    notes = this.notes
)