package com.tuorg.fleetcare.auth

import com.tuorg.fleetcare.api.dto.LoginRequest
import com.tuorg.fleetcare.api.dto.LoginResponse
import com.tuorg.fleetcare.api.dto.RegisterRequest
import com.tuorg.fleetcare.security.JwtUtil
import com.tuorg.fleetcare.user.domain.Role
import com.tuorg.fleetcare.user.domain.User
import com.tuorg.fleetcare.user.repo.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val repo: UserRepository,
    private val encoder: PasswordEncoder,
    private val jwt: JwtUtil
) {
    fun register(req: RegisterRequest): LoginResponse {
        require(req.email.isNotBlank() && req.password.length >= 6) { "Datos inválidos" }
        require(!repo.existsByEmail(req.email.lowercase())) { "Email ya registrado" }
        val user = User(
            email = req.email.lowercase(),
            passwordHash = encoder.encode(req.password),
            roles = mutableSetOf(Role.USER),
            areas = req.areas.toMutableSet()
        )
        val saved = repo.save(user)
        val token = jwt.generate(
            userId = saved.id!!,
            email = saved.email,
            roles = saved.roles.map { it.name },
            areas = saved.areas.map { it.name }
        )
        return LoginResponse(
            token = token,
            roles = user.roles.map { it.name },
            areas = user.areas.map { it.name }
        )
    }

    fun login(req: LoginRequest): LoginResponse {
        val user = repo.findByEmail(req.email.lowercase()).orElseThrow { IllegalArgumentException("Credenciales inválidas") }
        require(encoder.matches(req.password, user.passwordHash)) { "Credenciales inválidas" }
        val token = jwt.generate(
            userId = user.id!!,
            email = user.email,
            roles = user.roles.map { it.name },
            areas = user.areas.map { it.name }
        )
        return LoginResponse(
            token = token,
            roles = user.roles.map { it.name },
            areas = user.areas.map { it.name }
        )
    }
}