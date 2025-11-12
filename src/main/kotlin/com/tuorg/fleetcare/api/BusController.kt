package com.tuorg.fleetcare.api

import com.tuorg.fleetcare.bus.Bus
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
}