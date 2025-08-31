package com.tuorg.fleetcare.api.dto


data class AuthoritiesDto(
    val email: String,
    val authorities: List<String>
)