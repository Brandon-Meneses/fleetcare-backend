package com.tuorg.fleetcare.api.dto

import com.tuorg.fleetcare.user.domain.Area

data class CreateUserRequest(
    val email: String,
    val role: String,
    val area: Area? = null
)