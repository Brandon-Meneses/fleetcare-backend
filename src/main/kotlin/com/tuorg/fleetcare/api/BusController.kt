package com.tuorg.fleetcare.api

import com.tuorg.fleetcare.bus.Bus
import com.tuorg.fleetcare.bus.BusStatus
import com.tuorg.fleetcare.service.AutoScheduleService
import com.tuorg.fleetcare.service.BusService
import com.tuorg.fleetcare.service.PredictionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

data class CreateBusReq(val plate: String, val kmInitial: Long, val dateEnabled: String?)
data class UpdateKmReq(val km: Long)
data class UpdateLastMaintReq(val date: String)
data class PredictionRes(val dateKm: String?, val dateTime: String?, val finalDate: String?, val note: String?)
data class UpdateBusStatusReq(val status: String, val replacementId: String?)
data class UpdateBusReq(
    val plate: String,
    val kmCurrent: Long,
    val lastMaintenance: String?,
    val alias: String?,
    val notes: String?
)


@RestController
@RequestMapping("/buses")
class BusController(
    private val busService: BusService,
    private val prediction: PredictionService,
    private val auto: AutoScheduleService
) {
    @GetMapping fun list(): List<Bus> = busService.list()

    @PostMapping
    fun create(@RequestBody r: CreateBusReq): ResponseEntity<Bus> =
        ResponseEntity.ok(
            busService.create(
                r.plate.trim(),
                r.kmInitial,
                r.dateEnabled?.let { LocalDate.parse(it) }
            )
        )

    @PutMapping("/{id}/km")
    fun updateKm(@PathVariable id: String, @RequestBody r: UpdateKmReq) =
        ResponseEntity.ok(busService.updateKm(id, r.km))

    @PutMapping("/{id}/last-maint")
    fun updateLastMaint(@PathVariable id: String, @RequestBody r: UpdateLastMaintReq) =
        ResponseEntity.ok(busService.updateLastMaintenance(id, LocalDate.parse(r.date)))

    @GetMapping("/{id}/prediction")
    fun predict(@PathVariable id: String, @RequestParam(required=false) kmPerDay: Long?) =
        prediction.predict(id, kmPerDay).let { p ->
            ResponseEntity.ok(
                PredictionRes(
                    p.dateKm?.toString(), p.dateTime?.toString(), p.finalDate?.toString(), p.note
                )
            )
        }

    @PostMapping("/{id}/auto-schedule")
    fun autoSchedule(@PathVariable id: String, @RequestParam(required=false) adjustDays: Long?) =
        ResponseEntity.ok(auto.scheduleByPrediction(id, adjustDays))


    @PutMapping("/{id}/status")
    fun updateStatus(
        @PathVariable id: String,
        @RequestBody r: UpdateBusStatusReq
    ): ResponseEntity<Bus> {
        val status = try {
            BusStatus.valueOf(r.status.uppercase())
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest().build()
        }
        return ResponseEntity.ok(busService.updateStatus(id, status, r.replacementId))
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: String): ResponseEntity<Void> {
        return if (busService.delete(id)) {
            ResponseEntity.noContent().build() // 204
        } else {
            ResponseEntity.notFound().build() // 404
        }
    }

    @GetMapping("/{id}/next-maintenance")
    fun nextMaintenance(@PathVariable id: String): ResponseEntity<Map<String, String?>> {
        val p = prediction.predict(id)

        val resp = mapOf(
            "busId" to id,
            "nextMaintenance" to p.finalDate?.toString(),
            "reason" to p.note
        )

        return ResponseEntity.ok(resp)
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: String,
        @RequestBody r: UpdateBusReq
    ): ResponseEntity<Bus> {

        val lastMaint = r.lastMaintenance?.let { LocalDate.parse(it) }

        val updated = busService.updateGeneral(
            id = id,
            plate = r.plate,
            km = r.kmCurrent,
            lastMaint = lastMaint,
            alias = r.alias,
            notes = r.notes
        )

        return ResponseEntity.ok(updated)
    }
}