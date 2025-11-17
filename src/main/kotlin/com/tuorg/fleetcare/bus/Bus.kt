package com.tuorg.fleetcare.bus

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "buses")
data class Bus(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: String? = null,

    @Column(unique = true, nullable = false)
    val plate: String,

    @Column(nullable = false)
    val kmCurrent: Long = 0,

    // Última fecha de mantenimiento
    val lastMaintenanceDate: LocalDate? = null,

    val nextMaintenanceDate: LocalDate? = null,

    // Alias opcional del bus (ej: "EXPRESO 7", "BUS ROJO")
    val alias: String? = null,

    // Notas adicionales del bus
    @Column(length = 2000)
    val notes: String? = null,

    // Estado administrativo-operativo
    @Enumerated(EnumType.STRING)
    val status: BusStatus = BusStatus.OK,

    // Si fue reemplazado, referencia al bus nuevo
    val replacementId: String? = null
)

enum class BusStatus {
    OK,           // Operativo y sin mantenimiento pendiente
    PROXIMO,      // Próximo a mantenimiento
    VENCIDO,      // Mantenimiento vencido
    FUERA_SERVICIO, // Dado de baja o en retiro definitivo
    REEMPLAZADO     // Bus reemplazado por otro
}