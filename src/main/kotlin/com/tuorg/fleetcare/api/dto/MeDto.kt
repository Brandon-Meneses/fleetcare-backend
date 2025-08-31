package com.tuorg.fleetcare.api.dto

data class MeDto(
    val id: Long,
    val email: String,
    val roles: List<String>,
    val areas: List<String>
)