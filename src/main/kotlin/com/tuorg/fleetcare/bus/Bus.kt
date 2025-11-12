package com.tuorg.fleetcare.bus

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "buses")
data class Bus(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    val id: String? = null,

    @Column(unique = true, nullable = false)
    val plate: String,

    @Column(nullable = false)
    val kmCurrent: Long = 0,

    val lastMaintenanceDate: LocalDate? = null,

    // 🔹 Estado operativo y administrativo del bus
    @Enumerated(EnumType.STRING)
    val status: BusStatus = BusStatus.OK,

    // 🔹 Si el bus fue reemplazado, referencia al nuevo bus
    val replacementId: String? = null
)

enum class BusStatus {
    OK,           // Operativo y sin mantenimiento pendiente
    PROXIMO,      // Próximo a mantenimiento
    VENCIDO,      // Mantenimiento vencido
    FUERA_SERVICIO, // Dado de baja o en retiro definitivo
    REEMPLAZADO     // Bus reemplazado por otro
}