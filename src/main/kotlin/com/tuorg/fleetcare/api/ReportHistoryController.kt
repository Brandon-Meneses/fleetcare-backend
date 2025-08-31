package com.tuorg.fleetcare.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.tuorg.fleetcare.api.dto.ReportResponse
import com.tuorg.fleetcare.report.ReportRepository
import com.tuorg.fleetcare.user.domain.Area
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "Reportes", description = "Historial de reportes generados por área")
@SecurityRequirement(name = "bearer-jwt")
@RestController
@RequestMapping("/report/history")
class ReportHistoryController(
    private val repo: ReportRepository,
    private val mapper: ObjectMapper
) {
    @Operation(
        summary = "Obtener historial de reportes por área",
        description = "Devuelve todos los reportes guardados de un área específica (OPERATIONS, FINANCE, MAINTENANCE, COMMERCIAL).",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Lista de reportes encontrados",
                content = [Content(array = ArraySchema(schema = Schema(implementation = ReportResponse::class)))]
            ),
            ApiResponse(responseCode = "401", description = "No autorizado"),
            ApiResponse(responseCode = "403", description = "Acceso denegado"),
            ApiResponse(responseCode = "404", description = "No se encontró el recurso")
        ]
    )
    @GetMapping("/{area}")
    fun byArea(@PathVariable area: Area): ResponseEntity<List<ReportResponse>> {
        val list = repo.findByAreaOrderByCreatedAtDesc(area)
            .map { mapper.readValue(it.payloadJson, ReportResponse::class.java) }
        return ResponseEntity.ok(list)
    }
}