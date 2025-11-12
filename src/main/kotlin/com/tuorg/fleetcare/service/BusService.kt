package com.tuorg.fleetcare.service

import com.tuorg.fleetcare.bus.*
import com.tuorg.fleetcare.notify.Notification
import com.tuorg.fleetcare.notify.NotificationRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
class BusService(
    private val repo: BusRepository,
    private val notifications: NotificationRepository,
    @Value("\${rules.km.threshold:50000}") private val kmThreshold: Long,
    @Value("\${rules.km.maxAnnual:200000}") private val kmMaxAnnual: Long,
    @Value("\${rules.days.threshold:90}") private val daysThreshold: Long,
    @Value("\${rules.days.maxBetween:365}") private val daysMaxBetween: Long
) {

    fun get(id: String) = repo.findById(id).orElseThrow { IllegalArgumentException("Bus no encontrado") }
    fun list(): List<Bus> = repo.findAll()

    @Transactional
    fun create(plate: String, kmInitial: Long, dateEnabled: LocalDate?): Bus {
        require(plate.isNotBlank()) { "Placa requerida" }
        require(!repo.existsByPlate(plate)) { "Placa duplicada" }
        require(kmInitial in 0..kmMaxAnnual) { "Kilometraje fuera de rango permitido (0..$kmMaxAnnual)" }
        val saved = repo.save(Bus(plate = plate, kmCurrent = kmInitial, lastMaintenanceDate = dateEnabled))
        return recalcAndPersist(saved)
    }

    @Transactional
    fun updateKm(id: String, newKm: Long): Bus {
        val bus = get(id)
        require(newKm >= bus.kmCurrent) { "No se permite disminuir kilometraje" }
        require(newKm <= kmMaxAnnual) { "Kilometraje fuera de rango permitido (≤ $kmMaxAnnual)" }
        val updated = bus.copy(kmCurrent = newKm)
        return recalcAndPersist(updated)
    }

    @Transactional
    fun updateLastMaintenance(id: String, last: LocalDate): Bus {
        val bus = get(id)
        val days = ChronoUnit.DAYS.between(last, LocalDate.now())
        require(days <= daysMaxBetween) { "Fecha de mantenimiento excede el máximo permitido ($daysMaxBetween días)" }
        val updated = bus.copy(lastMaintenanceDate = last)
        return recalcAndPersist(updated)
    }

    private fun recalcAndPersist(b: Bus): Bus {
        val newStatus = classify(b.kmCurrent, b.lastMaintenanceDate)
        val withStatus = b.copy(status = newStatus)
        val saved = repo.save(withStatus)
        maybeNotify(saved, newStatus)
        return saved
    }

    // Regla HU4: OK <90%, PROXIMO 90–100%, VENCIDO ≥100% o por tiempo
    private fun classify(km: Long, last: LocalDate?): BusStatus {
        val byKm = when {
            km < (0.9 * kmThreshold).toLong() -> BusStatus.OK
            km in (0.9 * kmThreshold).toLong()..kmThreshold -> BusStatus.PROXIMO
            else -> BusStatus.VENCIDO
        }
        val byDays = last?.let {
            val d = ChronoUnit.DAYS.between(it, LocalDate.now())
            when {
                d < (0.9 * daysThreshold).toLong() -> BusStatus.OK
                d in (0.9 * daysThreshold).toLong()..daysThreshold -> BusStatus.PROXIMO
                else -> BusStatus.VENCIDO
            }
        } ?: BusStatus.PROXIMO // si no hay fecha, marcamos PROXIMO por datos incompletos
        // toma el peor estado entre km y tiempo
        return listOf(byKm, byDays).maxBy { order(it) }
    }

    private fun order(s: BusStatus) = when(s){ BusStatus.OK->0; BusStatus.PROXIMO->1; BusStatus.VENCIDO->2; BusStatus.FUERA_SERVICIO->3 }

    private fun maybeNotify(bus: Bus, status: BusStatus) {
        if (status == BusStatus.PROXIMO || status == BusStatus.VENCIDO) {
            notifications.save(
                Notification(
                    userEmail = "example@example.com",
                    title = "Mantenimiento ${status.name.lowercase()}",
                    content = "El bus con placa ${bus.plate} requiere atención de mantenimiento.",
                    link = "/buses/${bus.id}",
                    read = false
                )
            )
        }
    }
}