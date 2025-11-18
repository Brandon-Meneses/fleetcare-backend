package com.tuorg.fleetcare.service

import com.tuorg.fleetcare.bus.BusStatus
import com.tuorg.fleetcare.maintanance.*
import com.tuorg.fleetcare.notify.Notification
import com.tuorg.fleetcare.notify.NotificationRepository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class AutoScheduleService(
    private val pred: PredictionService,
    private val repo: MaintenanceOrderRepository,
    private val notifications: NotificationRepository,
    private val busService: BusService
) {
    @Transactional
    fun scheduleByPrediction(busId: String, adjustDays: Long? = null): MaintenanceOrder {
        val bus = busService.get(busId)

        // 1) No agendar para FUERA_SERVICIO / REEMPLAZADO
        require(bus.status != BusStatus.FUERA_SERVICIO && bus.status != BusStatus.REEMPLAZADO) {
            "No se puede auto-agendar mantenimiento para un bus ${bus.status}"
        }

        // 2) No auto-agendar si ya hay orden OPEN
        require(!repo.existsByBusIdAndStatus(busId, MaintenanceStatus.OPEN)) {
            "Ya existe una orden ABIERTA para este bus"
        }

        // 3) (Opcional) No auto-agendar si ya hay una PLANIFICADA
        require(!repo.existsByBusIdAndStatus(busId, MaintenanceStatus.PLANNED)) {
            "Ya existe una orden PLANIFICADA para este bus"
        }

        val p = pred.predict(busId)
        requireNotNull(p.finalDate) { "Datos insuficientes para agendar" }

        require(adjustDays == null || adjustDays in -7..7) {
            "Ajuste permitido ±7 días"
        }

        val date = p.finalDate!!.atStartOfDay().plusDays(adjustDays ?: 0)

        notifications.save(
            Notification(
                userEmail = SecurityContextHolder.getContext().authentication.name,
                title = "Orden auto-agendada",
                content = "Se creó automáticamente una orden de mantenimiento para el bus ${bus.plate}.",
                link = "/maintenance?busId=$busId",
                read = false
            )
        )

        return repo.save(
            MaintenanceOrder(
                busId = busId,
                type = MaintenanceType.PREVENTIVE,
                status = MaintenanceStatus.PLANNED,
                plannedAt = date
            )
        )
    }
}