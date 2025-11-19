package com.tuorg.fleetcare.api

import com.tuorg.fleetcare.api.dto.CreateUserRequest
import com.tuorg.fleetcare.service.EmailService
import com.tuorg.fleetcare.user.domain.Area
import com.tuorg.fleetcare.user.domain.Role
import com.tuorg.fleetcare.user.domain.User
import com.tuorg.fleetcare.user.repo.UserRepository
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.SecureRandom
import java.util.Base64


@Tag(name = "Admin Usuarios", description = "Creación de usuarios y administradores")
@RestController
@RequestMapping("/admin/users")
class AdminUserController(
    private val userRepo: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val emailService: EmailService
) {

    @PostMapping
    fun create(@RequestBody req: CreateUserRequest): ResponseEntity<Any> {

        val existingUsers = userRepo.count()

        // ============ 1️⃣ Si NO hay usuarios → modo bootstrap ============
        if (existingUsers == 0L) {

            val email = req.email.lowercase()
            val rawPassword = generateSecureToken()
            val hashedPassword = passwordEncoder.encode(rawPassword)

            val saved = userRepo.save(
                User(
                    email = email,
                    passwordHash = hashedPassword,
                    roles = mutableSetOf(Role.valueOf(req.role)),
                    areas = if (req.role == "USER") mutableSetOf(req.area!!) else mutableSetOf()
                )
            )

            emailService.send(
                to = saved.email,
                subject = "Bienvenido a FleetCare (Administrador inicial)",
                message = """
                    Hola ${saved.email},
                    
                    Tu cuenta inicial ha sido creada.
                    Contraseña temporal:
                    
                    $rawPassword
                    
                    Por favor inicia sesión y cambia tu contraseña pronto.
                """.trimIndent()
            )

            return ResponseEntity.ok(
                mapOf(
                    "mode" to "bootstrap",
                    "message" to "Primer usuario creado correctamente",
                    "id" to saved.id,
                    "email" to saved.email,
                    "roles" to saved.roles.map { it.name },
                    "areas" to saved.areas.map { it.name }
                )
            )
        }

        // ============ 2️⃣ Si ya existen usuarios → requiere ADMIN ============
        val auth = SecurityContextHolder.getContext().authentication
            ?: return ResponseEntity.status(401).body(
                mapOf("error" to "Se requiere autenticación")
            )

        if (!auth.authorities.any { it.authority == "ROLE_ADMIN" }) {
            return ResponseEntity.status(403).body(
                mapOf("error" to "Solo ADMIN puede crear usuarios")
            )
        }

        // ============ Validaciones normales ============
        val email = req.email.lowercase()

        if (userRepo.existsByEmail(email)) {
            return ResponseEntity.status(409).body(mapOf("error" to "Email ya registrado"))
        }

        if (req.role == "USER" && req.area == null) {
            return ResponseEntity.badRequest().body(
                mapOf("error" to "El usuario con rol USER requiere área")
            )
        }

        val rawPassword = generateSecureToken()
        val hashedPassword = passwordEncoder.encode(rawPassword)

        val saved = userRepo.save(
            User(
                email = email,
                passwordHash = hashedPassword,
                roles = mutableSetOf(Role.valueOf(req.role)),
                areas = if (req.role == "USER") mutableSetOf(req.area!!) else mutableSetOf()
            )
        )

        emailService.send(
            to = saved.email,
            subject = "Tu cuenta FleetCare ha sido creada",
            message = """
                Hola ${saved.email},
                
                Tu cuenta ha sido creada exitosamente.
                Contraseña temporal:
                
                $rawPassword
                
                Inicia sesión y actualízala pronto.
            """.trimIndent()
        )

        return ResponseEntity.ok(
            mapOf(
                "mode" to "admin",
                "id" to saved.id,
                "email" to saved.email,
                "roles" to saved.roles.map { it.name },
                "areas" to saved.areas.map { it.name }
            )
        )
    }
}

fun generateSecureToken(): String {
    val bytes = ByteArray(32)
    SecureRandom().nextBytes(bytes)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}