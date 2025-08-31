package com.tuorg.fleetcare.api

import com.tuorg.fleetcare.api.dto.NotificationDto
import com.tuorg.fleetcare.api.dto.PageResponse
import com.tuorg.fleetcare.notify.NotificationRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.format.datetime.standard.DateTimeFormatterFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.time.format.DateTimeFormatter

@Tag(name = "Notificaciones")
@SecurityRequirement(name = "bearer-jwt")
@RestController
@RequestMapping("/notifications")
class NotificationController(
    private val repo: NotificationRepository
) {
    private val iso = DateTimeFormatter.ISO_INSTANT

    // GET /notifications?page=0&size=20
    @GetMapping
    fun unread(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PageResponse<NotificationDto>> {
        val email = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(401).build()

        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val p = repo.findByUserEmailAndReadIsFalse(email, pageable)

        val items = p.content.map {
            NotificationDto(
                id = it.id!!,
                title = it.title,
                content = it.content,
                link = it.link,
                createdAt = iso.format(it.createdAt)
            )
        }

        val body = PageResponse(
            items = items,
            page = p.number,
            size = p.size,
            totalItems = p.totalElements,
            totalPages = p.totalPages,
            hasNext = p.hasNext(),
            hasPrevious = p.hasPrevious()
        )
        return ResponseEntity.ok(body)
    }

    // PATCH /notifications/{id}/read  → marcar una como leída
    @PatchMapping("/{id}/read")
    @Transactional
    fun markRead(@PathVariable id: Long): ResponseEntity<Void> {
        val email = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(401).build()

        val n = repo.findById(id).orElse(null) ?: return ResponseEntity.notFound().build()
        if (n.userEmail != email) return ResponseEntity.status(403).build()

        if (!n.read) {
            n.read = true
            repo.save(n)
        }
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "Cantidad de notificaciones NO leídas del usuario")
    @GetMapping("/count")
    fun unreadCount(): ResponseEntity<Map<String, Long>> {
        val email = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(401).build()
        val count = repo.countByUserEmailAndReadIsFalse(email)
        return ResponseEntity.ok(mapOf("count" to count))
    }
}