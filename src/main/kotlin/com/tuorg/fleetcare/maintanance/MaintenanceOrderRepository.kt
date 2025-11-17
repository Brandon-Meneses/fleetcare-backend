package com.tuorg.fleetcare.maintanance

import org.springframework.data.jpa.repository.JpaRepository

interface MaintenanceOrderRepository : JpaRepository<MaintenanceOrder, String> {
    fun findByBusId(busId: String): List<MaintenanceOrder>
    fun existsByBusIdAndStatus(busId: String, status: MaintenanceStatus): Boolean

}