package com.tuorg.fleetcare.bus

import org.springframework.data.jpa.repository.JpaRepository

interface BusRepository : JpaRepository<Bus, String> {
    fun findByPlate(plate: String): Bus?
    fun existsByPlate(plate: String): Boolean
}