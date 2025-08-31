package com.tuorg.fleetcare.api

import com.tuorg.fleetcare.api.dto.LoginRequest
import com.tuorg.fleetcare.api.dto.LoginResponse
import com.tuorg.fleetcare.api.dto.RegisterRequest
import com.tuorg.fleetcare.auth.AuthService
import com.tuorg.fleetcare.security.JwtUtil
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

data class LoginRequest(val email: String = "", val password: String = "")
data class LoginResponse(val token: String)

@Tag(name = "Auth")
@RestController
@RequestMapping("/auth")
class AuthController(private val auth: AuthService) {


    @Operation(summary = "Registro de usuario")
    @PostMapping("/register")
    fun register(@RequestBody req: RegisterRequest): ResponseEntity<LoginResponse> =
        ResponseEntity.ok(auth.register(req))

    @Operation(summary = "Login (devuelve JWT)")
    @PostMapping("/login")
    fun login(@RequestBody req: LoginRequest): ResponseEntity<LoginResponse> =
        ResponseEntity.ok(auth.login(req))
}