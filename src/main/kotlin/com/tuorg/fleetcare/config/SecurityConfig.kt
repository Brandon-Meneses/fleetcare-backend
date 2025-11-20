package com.tuorg.fleetcare.config

import com.tuorg.fleetcare.security.JwtAuthEntryPoint
import com.tuorg.fleetcare.security.JwtAuthFilter
import com.tuorg.fleetcare.security.JwtUtil
import com.tuorg.fleetcare.user.repo.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.Customizer.withDefaults
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.cors.CorsConfigurationSource

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig(
    @Value("\${app.cors.allowed-origins}") private val allowedOrigins: String,
    @Value("\${app.jwt.secret}") private val jwtSecret: String,
    @Value("\${app.jwt.exp-minutes}") private val jwtExpMinutes: Long,
    private val userRepo: UserRepository // <-- Importante añadir esto
) {

    @Bean
    fun jwtUtil() = JwtUtil(jwtSecret, jwtExpMinutes)

    @Bean
    fun jwtAuthFilter(jwtUtil: JwtUtil) = JwtAuthFilter(jwtUtil)

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        jwtAuthFilter: JwtAuthFilter,
        jwtAuthEntryPoint: JwtAuthEntryPoint
    ): SecurityFilterChain {

        val noUsers = userRepo.count() == 0L

        http
            .csrf { it.disable() }
            .cors { }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling { it.authenticationEntryPoint(jwtAuthEntryPoint) }
            .authorizeHttpRequests { auth ->

                // ----------------------------------------------------------
                //  Si NO hay usuarios → permitir crear el primer ADMIN
                // ----------------------------------------------------------
                if (noUsers) {
                    auth.requestMatchers(HttpMethod.POST, "/admin/users").permitAll()
                } else {
                    // ------------------------------------------------------
                    // 2️⃣ Si ya existe al menos uno → requiere token ADMIN
                    // ------------------------------------------------------
                    auth.requestMatchers(HttpMethod.POST, "/admin/users").hasAuthority("ADMIN")
                }

                // ----------------------------------------------------------
                // Rutas libres
                // ----------------------------------------------------------
                auth.requestMatchers("/auth/**").permitAll()
                auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                auth.requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                auth.requestMatchers("/actuator/**").permitAll()

                // ----------------------------------------------------------
                // Todo lo demás requiere autenticación
                // ----------------------------------------------------------
                auth.anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val cfg = CorsConfiguration()
        cfg.allowedOriginPatterns = allowedOrigins.split(",").map { it.trim() }
        cfg.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        cfg.allowedHeaders = listOf("*")
        cfg.exposedHeaders = listOf("Authorization")
        cfg.allowCredentials = true

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", cfg)
        return source
    }
}