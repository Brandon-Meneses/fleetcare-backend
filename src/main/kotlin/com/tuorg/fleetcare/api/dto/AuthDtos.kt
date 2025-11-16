package com.tuorg.fleetcare.api.dto

import com.tuorg.fleetcare.user.domain.Area


data class RegisterRequest(
    val email: String,
    val password: String,
    val areas: List<Area> = emptyList()
)
data class LoginRequest(val email: String, val password: String)
data class LoginResponse(
    val token: String,
    val roles: List<String>,
    val areas: List<String>
)