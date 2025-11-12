package com.tuorg.fleetcare.bus

import jakarta.persistence.*
import java.time.LocalDate

@Entity @Table(name = "buses")
data class Bus(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    val id: String? = null,

    @Column(unique = true, nullable = false)
    val plate: String,

    @Column(nullable = false)
    val kmCurrent: Long = 0,

    val lastMaintenanceDate: LocalDate? = null,

    @Enumerated(EnumType.STRING)
    val status: BusStatus = BusStatus.OK
)

enum class BusStatus { OK, PROXIMO, VENCIDO, FUERA_SERVICIO }