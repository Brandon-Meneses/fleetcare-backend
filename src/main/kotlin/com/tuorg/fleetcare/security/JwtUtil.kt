package com.tuorg.fleetcare.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*

@Component
class JwtUtil(
    @Value("\${app.jwt.secret}") private val secret: String,
    @Value("\${app.jwt.exp-minutes}") private val expMinutes: Long
) {
    // 👇 especifica el tipo para que Kotlin infiera bien
    private val alg: Algorithm by lazy { Algorithm.HMAC256(secret) }

    fun generate(userId: Long, email: String, roles: Collection<String>, areas: Collection<String>): String =
        JWT.create()
            .withSubject(email)
            .withClaim("uid", userId)
            .withArrayClaim("roles", roles.toTypedArray())
            .withArrayClaim("areas", areas.toTypedArray())
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + expMinutes * 60_000))
            .sign(alg)

    fun decode(token: String): DecodedJWT = JWT.require(alg).build().verify(token)

    fun validate(token: String): String? = try {
        JWT.require(alg).build().verify(token).subject
    } catch (_: Exception) { null }
}