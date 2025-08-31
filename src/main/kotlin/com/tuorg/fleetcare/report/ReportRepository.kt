package com.tuorg.fleetcare.report

import com.tuorg.fleetcare.user.domain.Area
import org.springframework.data.jpa.repository.JpaRepository

interface ReportRepository : JpaRepository<ReportEntity, Long> {
    fun findByAreaOrderByCreatedAtDesc(area: Area): List<ReportEntity>
    fun findByDataHash(dataHash: String): List<ReportEntity>
}