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
import com.tuorg.fleetcare.user.domain.User
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
        val auth = SecurityContextHolder.getContext().authentication
        val area = (auth.authorities.firstOrNull { it.authority.startsWith("AREA_") }
            ?.authority?.removePrefix("AREA_")
            ?.let { Area.valueOf(it) })
            ?: Area.MAINTENANCE

        // Guardar historial (área opcional; si no aplica, puedes usar null o un área "GENERIC")
        reportRepository.save(
            ReportEntity(
                area = area,
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
    //@PreAuthorize("hasAuthority('AREA_' + #area)")
    @Transactional
    fun generateForArea(
        @PathVariable area: Area,
        @Valid @RequestBody req: ReportRequest
    ): ResponseEntity<ReportResponse> {

        val prompt = buildAreaPrompt(area, req)
        val resp = callGroqAndMap(req, prompt)

        // 1) Guardar historial
        reportRepository.save(
            ReportEntity(
                area = area,
                dataHash = resp.dataHash,
                payloadJson = objectMapper.writeValueAsString(resp)
            )
        )

        // 2) Info del usuario autenticado
        val authentication = SecurityContextHolder.getContext().authentication
        val actor = authentication?.name ?: "system"

        // 3) Determinar si es ADMIN
        val isAdmin = authentication
            ?.authorities
            ?.any { it.authority.equals("ADMIN", ignoreCase = true) }
            ?: false

        // 4) Lista de destinatarios
        val recipients: List<User> = if (isAdmin) {
            // ADMIN → notifica a todo el área
            userRepository.findAllByAreasContaining(area)
        } else {
            // USER → solo a sí mismo
            userRepository.findByEmail(actor)
                .map { listOf(it) }
                .orElse(emptyList())
        }

        val link = "/report/history/$area"
        val title = "Reporte $area generado"
        val content = "Se generó un informe del área $area (dataHash=${resp.dataHash})."

        // 5) Enviar notificaciones
        recipients.forEach { u ->
            notificationRepository.save(
                Notification(
                    userEmail = u.email,   // tu entidad usa exactamente "email"
                    title = title,
                    content = content,
                    link = link
                )
            )
        }

        // 6) Si ADMIN generó el reporte pero NO está en recipients (caso raro)
        if (isAdmin && recipients.none { it.email == actor }) {
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
    fun getUserArea(): Area {
        val auth = SecurityContextHolder.getContext().authentication
        val areaAuth = auth.authorities
            .firstOrNull { it.authority.startsWith("AREA_") }
            ?.authority?.removePrefix("AREA_")

        return if (areaAuth != null) Area.valueOf(areaAuth) else Area.MAINTENANCE
    }

    private fun buildGenericPrompt(req: ReportRequest): String {
        val area = getUserArea()
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

        Responde SOLO JSON válido.
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