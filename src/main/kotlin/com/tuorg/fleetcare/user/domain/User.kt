package com.tuorg.fleetcare.user.domain

import jakarta.persistence.*

@Entity @Table(name = "users")
class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(unique = true, nullable = false)
    var email: String,

    @Column(nullable = false)
    var passwordHash: String,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = [JoinColumn(name = "user_id")])
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    var roles: MutableSet<Role> = mutableSetOf(Role.USER),

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_areas", joinColumns = [JoinColumn(name = "user_id")])
    @Enumerated(EnumType.STRING)
    @Column(name = "area")
    var areas: MutableSet<Area> = mutableSetOf()
)