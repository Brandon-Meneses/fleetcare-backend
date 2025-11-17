package com.tuorg.fleetcare.service

import com.tuorg.fleetcare.api.dto.MaintenanceOrderRequest
import com.tuorg.fleetcare.api.dto.MaintenanceOrderResponse
import com.tuorg.fleetcare.bus.BusStatus
import com.tuorg.fleetcare.maintanance.MaintenanceOrder
import com.tuorg.fleetcare.maintanance.MaintenanceOrderRepository
import com.tuorg.fleetcare.maintanance.MaintenanceStatus
import com.tuorg.fleetcare.notify.NotificationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class MaintenanceService(
    private val repo: MaintenanceOrderRepository,
    private val busService: BusService,
    private val notifications: NotificationRepository,
) {
    fun listAll(): List<MaintenanceOrderResponse> =
        repo.findAll().map { it.toResponse() }

    fun listByBus(busId: String): List<MaintenanceOrderResponse> =
        repo.findByBusId(busId).map { it.toResponse() }

    fun create(req: MaintenanceOrderRequest): MaintenanceOrderResponse {
        // 1) Obtener bus para validar su estado
        val bus = busService.get(req.busId)

        require(bus.status != BusStatus.FUERA_SERVICIO && bus.status != BusStatus.REEMPLAZADO) {
            "No se puede crear una orden para un bus ${bus.status}"
        }

        // 2) Regla: no permitir otra orden ABIERTA para el mismo bus
        require(!repo.existsByBusIdAndStatus(req.busId, MaintenanceStatus.OPEN)) {
            "Ya existe una orden ABIERTA para este bus"
        }

        // (opcional) Regla: evitar duplicar planificadas (misma fecha y tipo)
        // require(!repo.existsByBusIdAndStatus(req.busId, MaintenanceStatus.PLANNED)) {
        //     "Ya existe una orden PLANIFICADA para este bus"
        // }

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

        require(order.status != MaintenanceStatus.CLOSED) {
            "La orden ya fue cerrada previamente"
        }

        // No abrir si ya hay otra orden OPEN para el mismo bus (y esta todavía no es OPEN)
        if (order.status != MaintenanceStatus.OPEN) {
            require(!repo.existsByBusIdAndStatus(order.busId, MaintenanceStatus.OPEN)) {
                "Ya existe otra orden ABIERTA para este bus"
            }
        }

        val updated = order.copy(
            status = MaintenanceStatus.OPEN,
            openedAt = LocalDateTime.now()
        )
        return repo.save(updated).toResponse()
    }

//    fun close(id: String, notes: String?): MaintenanceOrderResponse {
//        val order = repo.findById(id).orElseThrow()
//        val updated = order.copy(
//            status = MaintenanceStatus.CLOSED,
//            closedAt = LocalDateTime.now(),
//            notes = notes ?: order.notes
//        )
//        return repo.save(updated).toResponse()
//    }

    @Transactional
    fun close(id: String, notes: String?): MaintenanceOrderResponse {
        val order = repo.findById(id).orElseThrow()

        require(order.status != MaintenanceStatus.CLOSED) {
            "La orden ya fue cerrada previamente"
        }

        val updated = order.copy(
            status = MaintenanceStatus.CLOSED,
            closedAt = LocalDateTime.now(),
            notes = notes ?: order.notes
        )

        // ✅ 1. Actualiza fecha de mantenimiento del bus
        busService.updateLastMaintenance(order.busId, LocalDate.now())

        // ✅ 2. Marca alertas vinculadas como atendidas
        notifications.findAll()
            .filter { it.content.contains(order.busId, ignoreCase = true) && !it.read }
            .forEach {
                it.read = true
                notifications.save(it)
            }

        // ✅ 3. Guarda y retorna DTO actualizado
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