package com.tuorg.fleetcare.service

import com.tuorg.fleetcare.maintanance.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class AutoScheduleService(
    private val pred: PredictionService,
    private val repo: MaintenanceOrderRepository
) {
    @Transactional
    fun scheduleByPrediction(busId: String, adjustDays: Long? = null): MaintenanceOrder {
        val p = pred.predict(busId)
        requireNotNull(p.finalDate) { "Datos insuficientes para agendar" }
        val date = p.finalDate!!.atStartOfDay().plusDays(adjustDays ?: 0)
        require(adjustDays == null || adjustDays in -7..7) { "Ajuste permitido ±7 días" }
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