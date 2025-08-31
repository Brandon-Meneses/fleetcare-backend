package com.tuorg.fleetcare.api.dto


import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class BusDto(
    @field:NotBlank val id: String,
    @field:NotBlank val plate: String,
    val kmCurrent: Int = 0,
    val lastServiceAt: String? = null, // ISO date string (ej: 2025-08-10)
    val alias: String? = null,
    val notes: String? = null,
)

// 🔹 Configuración de umbrales
data class ConfigDto(
    val kmThreshold: Int,
    val monthsThreshold: Int
)

// 🔹 Request del reporte (lo que envía el frontend a /report)
data class ReportRequest(
    val prompt: String? = null,  // prompt dinámico (para IA)
    @field:NotNull val fleet: List<BusDto>,
    @field:NotNull val config: ConfigDto,
    @field:NotBlank val dataHash: String
)

// 🔹 KPI refinado → ahora value es numérico
data class ReportKpi(
    val name: String,
    val value: Number   // Puede ser Int, Double, etc.
)

// 🔹 Sección narrativa
data class ReportSection(
    val title: String,
    val content: String
)

// 🔹 Respuesta del reporte generado
data class ReportResponse(
    val summary: String,
    val generatedAt: String? = null,  // ISO8601 timestamp opcional
    val status: String? = null,       // healthy / attention / critical
    val kpis: List<ReportKpi>,
    val sections: List<ReportSection>,
    val dataHash: String
)