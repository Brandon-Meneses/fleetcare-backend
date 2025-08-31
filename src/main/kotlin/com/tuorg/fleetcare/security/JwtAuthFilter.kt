package com.tuorg.fleetcare.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(private val jwtUtil: JwtUtil) : OncePerRequestFilter() {
    override fun doFilterInternal(req: HttpServletRequest, res: HttpServletResponse, chain: FilterChain) {
        val path = req.requestURI
        if (req.method.equals("OPTIONS", true) || path.startsWith("/auth/")) {
            chain.doFilter(req, res); return
        }
        val header = req.getHeader("Authorization")
        val token = if (header?.startsWith("Bearer ") == true) header.substring(7) else null
        if (token != null && SecurityContextHolder.getContext().authentication == null) {
            runCatching { jwtUtil.decode(token) }.onSuccess { jwt ->
                val email = jwt.subject
                val roles = jwt.getClaim("roles").asList(String::class.java) ?: emptyList()
                val areas = jwt.getClaim("areas").asList(String::class.java) ?: emptyList()
                val authorities = buildList {
                    addAll(roles.map { SimpleGrantedAuthority("ROLE_$it") })        // ROLE_ADMIN, ROLE_USER
                    addAll(areas.map { SimpleGrantedAuthority("AREA_$it") })        // AREA_OPERATIONS, ...
                }
                val principal = User(email, "", authorities)
                val auth = UsernamePasswordAuthenticationToken(principal, null, authorities)
                auth.details = WebAuthenticationDetailsSource().buildDetails(req)
                SecurityContextHolder.getContext().authentication = auth
            }
        }
        chain.doFilter(req, res)
    }
}