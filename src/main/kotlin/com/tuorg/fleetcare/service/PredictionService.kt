package com.tuorg.fleetcare.service

import com.tuorg.fleetcare.bus.Bus
import com.tuorg.fleetcare.bus.BusRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

@Service
class PredictionService(
    private val buses: BusRepository,
    @Value("\${rules.km.threshold:50000}") private val kmThreshold: Long,
    @Value("\${rules.days.threshold:90}") private val daysThreshold: Long,
    @Value("\${rules.km.dailyEstimate:120}") private val kmDailyEstimate: Long
) {

    // ============================================================
    //   🔵 Método que usa el CONTROLLER: recibe busId (NO tocar)
    // ============================================================
    fun predict(busId: String, kmPerDay: Long? = null): Prediction {
        val b: Bus = buses.findById(busId).orElseThrow()

        val perDay = (kmPerDay ?: kmDailyEstimate).coerceAtLeast(1)
        val remainingKm = (kmThreshold - b.kmCurrent).coerceAtLeast(0)
        val daysByKm = ceil(remainingKm / perDay.toDouble()).toLong()
        val dateKm = LocalDate.now().plusDays(daysByKm)

        val dateTime = b.lastMaintenanceDate?.plusDays(daysThreshold)

        val finalDate = listOfNotNull(dateKm, dateTime).minOrNull()
        val note = if (finalDate == null) "Datos insuficientes" else null

        return Prediction(busId, dateKm, dateTime, finalDate, note)
    }

    data class Prediction(
        val busId: String,
        val dateKm: LocalDate?,
        val dateTime: LocalDate?,
        val finalDate: LocalDate?,
        val note: String?
    )

    // ============================================================
    //   🔥 Método interno para BusService: recibe el bus completo
    // ============================================================
    fun predictNext(bus: Bus, kmPerDay: Long? = null): LocalDate? {
        val perDay = (kmPerDay ?: kmDailyEstimate).coerceAtLeast(1)

        val remainingKm = (kmThreshold - bus.kmCurrent).coerceAtLeast(0)
        val daysByKm = ceil(remainingKm / perDay.toDouble()).toLong()
        val dateKm = LocalDate.now().plusDays(daysByKm)

        val dateTime = bus.lastMaintenanceDate?.plusDays(daysThreshold)

        return listOfNotNull(dateKm, dateTime).minOrNull()
    }
}