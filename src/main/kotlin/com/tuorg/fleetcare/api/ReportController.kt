package com.tuorg.fleetcare.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.tuorg.fleetcare.api.dto.ReportKpi
import com.tuorg.fleetcare.api.dto.ReportRequest
import com.tuorg.fleetcare.api.dto.ReportResponse
import com.tuorg.fleetcare.api.dto.ReportSection
import com.tuorg.fleetcare.notify.Notification
import com.tuorg.fleetcare.notify.NotificationRepository
import com.tuorg.fleetcare.report.AreaPrompts
import com.tuorg.fleetcare.report.ReportEntity
import com.tuorg.fleetcare.report.ReportRepository
import com.tuorg.fleetcare.service.GroqService
import com.tuorg.fleetcare.user.domain.Area
import com.tuorg.fleetcare.user.repo.UserRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.transaction.Transactional
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.security.SecurityRequirement

@Tag(name = "Reportes")
@RestController
@RequestMapping("/report")
class ReportController(
    private val groqService: GroqService,
    private val objectMapper: ObjectMapper,
    private val reportRepository: ReportRepository,
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository
) {
    // Reporte genérico (requiere JWT por SecurityConfig)
    @Operation(summary = "Genera reporte general", security = [SecurityRequirement(name = "bearer-jwt")])
    @PostMapping
    @Transactional
    fun generate(@Valid @RequestBody req: ReportRequest): ResponseEntity<ReportResponse> {
        val prompt = buildGenericPrompt(req)
        val resp = callGroqAndMap(req, prompt)

        // Guardar historial (área opcional; si no aplica, puedes usar null o un área "GENERIC")
        reportRepository.save(
            ReportEntity(
                area = Area.MAINTENANCE, // o un enum/área por defecto si este endpoint es general
                dataHash = resp.dataHash,
                payloadJson = objectMapper.writeValueAsString(resp)
            )
        )

        // Notificar al usuario que lo generó (opcional)
        val actor = SecurityContextHolder.getContext().authentication?.name ?: "system"
        notificationRepository.save(
            Notification(
                userEmail = actor,
                title = "Reporte generado",
                content = "Se generó un informe general con dataHash=${resp.dataHash}",
                link = "/report/history/MAINTENANCE"
            )
        )

        return ResponseEntity.ok(resp)
    }

    // Reporte por área – exige que el usuario tenga la autoridad de esa área
    @Operation(summary = "Genera reporte por área", security = [SecurityRequirement(name = "bearer-jwt")])
    @PostMapping("/area/{area}")
    @PreAuthorize("hasAuthority('AREA_' + #area)")
    @Transactional
    fun generateForArea(
        @PathVariable area: Area,
        @Valid @RequestBody req: ReportRequest
    ): ResponseEntity<ReportResponse> {
        val prompt = buildAreaPrompt(area, req)
        val resp = callGroqAndMap(req, prompt)

        // 1) Guardar historial del reporte por área
        reportRepository.save(
            ReportEntity(
                area = area,
                dataHash = resp.dataHash,
                payloadJson = objectMapper.writeValueAsString(resp)
            )
        )

        // 2) Notificar a TODOS los usuarios del área
        val recipients = userRepository.findAllByAreasContaining(area)
        val link = "/report/history/$area" // ruta para consultar historial por área
        val title = "Reporte $area generado"
        val content = "Se generó un informe de mantenimiento para el área $area (dataHash=${resp.dataHash})."

        recipients.forEach { u ->
            notificationRepository.save(
                Notification(
                    userEmail = u.email,
                    title = title,
                    content = content,
                    link = link
                )
            )
        }

        // 3) (Opcional) notificar también al actor que lo disparó
        val actor = SecurityContextHolder.getContext().authentication?.name ?: "system"
        if (recipients.none { it.email == actor }) {
            notificationRepository.save(
                Notification(
                    userEmail = actor,
                    title = title,
                    content = "$content Enviado a ${recipients.size} usuario(s) del área.",
                    link = link
                )
            )
        }

        return ResponseEntity.ok(resp)
    }

    // ---------- helpers (sin cambios de lógica) ----------

    private fun callGroqAndMap(req: ReportRequest, prompt: String): ReportResponse {
        val json = groqService.generateJson(prompt)

        val summary = json["summary"] as? String ?: "Fallback: respuesta no fue JSON válido"
        val kpis = (json["kpis"] as? List<*>)?.mapNotNull {
            (it as? Map<*, *>)?.let { m ->
                val n = m["name"]?.toString()
                val v = m["value"]
                if (n != null && v != null) ReportKpi(n, when (v) {
                    is Number -> v
                    else -> v.toString().toIntOrNull() ?: 0
                }) else null
            }
        } ?: emptyList()
        val sections = (json["sections"] as? List<*>)?.mapNotNull {
            (it as? Map<*, *>)?.let { m ->
                val t = m["title"]?.toString()
                val c = m["content"]?.toString()
                if (t != null && c != null) ReportSection(t, c) else null
            }
        } ?: emptyList()

        val dataHash = (json["dataHash"] as? String) ?: req.dataHash
        val generatedAt = json["generatedAt"]?.toString()
        val status = json["status"]?.toString()

        return ReportResponse(
            summary = summary,
            generatedAt = generatedAt,
            status = status,
            kpis = kpis,
            sections = sections,
            dataHash = dataHash
        )
    }

    private fun buildGenericPrompt(req: ReportRequest): String {
        val schema = """
    Devuelve *EXCLUSIVAMENTE JSON válido*, sin explicaciones, sin código Markdown, sin texto adicional.
    Si no puedes generar el JSON, devuelve exactamente: {"error": "invalid-json"}.
    
    Estructura obligatoria:
    {
      "summary": "string",
      "generatedAt": "string",
      "status": "string",
      "kpis": [{"name": "string", "value": number}],
      "sections": [{"title": "string", "content": "string"}],
      "dataHash": "string"
    }
    """.trimIndent()

        val fleet = req.fleet.joinToString(",\n") {
            """{"plate":"${it.plate}","kmCurrent":${it.kmCurrent},"lastServiceAt":"${it.lastServiceAt ?: ""}"}"""
        }

        val cfg = """{"kmThreshold":${req.config.kmThreshold},"monthsThreshold":${req.config.monthsThreshold}}"""

        return """
        $schema
        
        Datos:
        "fleet": [$fleet],
        "config": $cfg,
        "dataHash": "${req.dataHash}"
        
        Responde SOLO con JSON. Nada más.
    """.trimIndent()
    }

    private fun buildAreaPrompt(area: Area, req: ReportRequest): String {
        val header = AreaPrompts.by(area)
        val fleet = req.fleet.joinToString(",\n") {
            """{"plate":"${it.plate}","kmCurrent":${it.kmCurrent},"lastServiceAt":"${it.lastServiceAt ?: ""}"}"""
        }
        val cfg = """{"kmThreshold":${req.config.kmThreshold},"monthsThreshold":${req.config.monthsThreshold}}"""
        return """
          $header

          Datos:
          "fleet": [$fleet],
          "config": $cfg,
          "dataHash": "${req.dataHash}"

          Incluye KPIs relevantes al área $area.
          Responde SOLO con JSON válido.
        """.trimIndent()
    }
}