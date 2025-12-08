package com.tuorg.fleetcare.service


import com.resend.Resend
import com.resend.services.emails.model.SendEmailRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class EmailService(

    @Value("\${resend.api.key}")
    private val apiKey: String

) {

    fun send(to: String, subject: String, message: String) {

        val resend = Resend(apiKey)

        val params = SendEmailRequest.builder()
            .from("FleetCare <onboarding@resend.dev>")
            .to(listOf(to))
            .subject(subject)
            .text(message)
            .build()

        resend.emails().send(params)
    }
}