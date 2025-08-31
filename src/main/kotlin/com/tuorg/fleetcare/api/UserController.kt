package com.tuorg.fleetcare.api

import com.tuorg.fleetcare.api.dto.AuthoritiesDto
import com.tuorg.fleetcare.api.dto.MeDto
import com.tuorg.fleetcare.user.repo.UserRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Usuarios", description = "Información del usuario autenticado")
@SecurityRequirement(name = "bearer-jwt")
@RestController
@RequestMapping("/me")
class UserController(
    private val userRepo: UserRepository
) {
    @Operation(summary = "Obtiene la información del usuario autenticado")
    @GetMapping
    fun me(): ResponseEntity<MeDto> {
        val email = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(401).build()

        val user = userRepo.findByEmail(email.lowercase()).orElse(null)
            ?: return ResponseEntity.status(404).build()

        val dto = MeDto(
            id = user.id!!,
            email = user.email,
            roles = user.roles.map { it.name },
            areas = user.areas.map { it.name }
        )
        return ResponseEntity.ok(dto)
    }

    @Operation(summary = "Obtiene las authorities exactas de Spring Security para el usuario actual")
    @GetMapping("/authorities")
    fun authorities(): ResponseEntity<AuthoritiesDto> {
        val auth = SecurityContextHolder.getContext().authentication
            ?: return ResponseEntity.status(401).build()

        val dto = AuthoritiesDto(
            email = auth.name,
            authorities = auth.authorities.map { it.authority }
        )
        return ResponseEntity.ok(dto)
    }
}