package com.tuorg.fleetcare.user.repo


import com.tuorg.fleetcare.user.domain.Area
import com.tuorg.fleetcare.user.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): Optional<User>
    fun existsByEmail(email: String): Boolean
    fun findAllByAreasContaining(area: Area): List<User>
}