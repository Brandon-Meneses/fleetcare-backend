package com.tuorg.fleetcare.api

import com.tuorg.fleetcare.api.dto.MaintenanceOrderRequest
import com.tuorg.fleetcare.api.dto.MaintenanceOrderResponse
import com.tuorg.fleetcare.service.MaintenanceService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "Mantenimiento", description = "Gestión de órdenes de mantenimiento")
@SecurityRequirement(name = "bearer-jwt")
@RestController
@RequestMapping("/maintenance")
class MaintenanceController(
    private val service: MaintenanceService
) {
    @Operation(summary = "Lista todas las órdenes de mantenimiento")
    @GetMapping
    fun listAll(): ResponseEntity<List<MaintenanceOrderResponse>> =
        ResponseEntity.ok(service.listAll())

    @Operation(summary = "Lista las órdenes de mantenimiento de un bus específico")
    @GetMapping("/{busId}")
    fun listByBus(@PathVariable busId: String): ResponseEntity<List<MaintenanceOrderResponse>> =
        ResponseEntity.ok(service.listByBus(busId))

    @Operation(summary = "Crea una nueva orden de mantenimiento")
    @PostMapping
    fun create(@RequestBody req: MaintenanceOrderRequest): ResponseEntity<MaintenanceOrderResponse> =
        ResponseEntity.ok(service.create(req))

    @Operation(summary = "Abre una orden de mantenimiento (cambia estado a OPEN)")
    @PatchMapping("/{id}/open")
    fun open(@PathVariable id: String): ResponseEntity<MaintenanceOrderResponse> =
        ResponseEntity.ok(service.open(id))

    @Operation(summary = "Cierra una orden de mantenimiento (cambia estado a CLOSED)")
    @PatchMapping("/{id}/close")
    fun close(
        @PathVariable id: String,
        @RequestBody body: Map<String, String?>
    ): ResponseEntity<MaintenanceOrderResponse> =
        ResponseEntity.ok(service.close(id, body["notes"]))
}