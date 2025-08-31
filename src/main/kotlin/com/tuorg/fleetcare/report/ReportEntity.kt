package com.tuorg.fleetcare.report


import com.tuorg.fleetcare.user.domain.Area
import jakarta.persistence.*
import java.time.Instant

@Entity @Table(name = "reports")
class ReportEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Enumerated(EnumType.STRING)
    var area: Area,
    var dataHash: String,
    @Lob
    var payloadJson: String,
    var createdAt: Instant = Instant.now()
)