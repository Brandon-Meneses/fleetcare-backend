package com.tuorg.fleetcare.service


import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class GroqService(
    @Value("\${groq.api.url}") private val apiUrl: String,
    @Value("\${groq.api.key}") private val apiKey: String?,
    @Value("\${groq.model}") private val model: String,
    private val mapper: ObjectMapper
) {
    private val client = WebClient.builder()
        .baseUrl(apiUrl)
        .defaultHeader("Authorization", "Bearer ${apiKey ?: ""}")
        .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        .build()

    fun generateJson(prompt: String): Map<String, Any?> {
        if (apiKey.isNullOrBlank()) {
            // Fallback local si no hay key
            return mapOf(
                "summary" to "Fallback local (sin GROQ_API_KEY).",
                "kpis" to listOf(mapOf("name" to "Ejemplo", "value" to "OK")),
                "sections" to listOf(mapOf("title" to "Nota", "content" to "Configura GROQ_API_KEY en el backend.")),
                "dataHash" to "no-key"
            )
        }

        val payload = mapOf(
            "model" to model,
            "temperature" to 0.2,
            "messages" to listOf(
                mapOf("role" to "system", "content" to "Responde SOLO con JSON válido, sin texto adicional."),
                mapOf("role" to "user", "content" to prompt)
            )
        )

        val resp = client.post()
            .bodyValue(payload)
            .retrieve()
            .bodyToMono(Map::class.java)
            .block()

        val content = ((resp?.get("choices") as? List<*>)?.firstOrNull() as? Map<*, *>)?.get("message") as? Map<*, *>
        val text = content?.get("content")?.toString() ?: "{}"

        return try {
            @Suppress("UNCHECKED_CAST")
            mapper.readValue(text, Map::class.java) as Map<String, Any?>
        } catch (ex: Exception) {
            mapOf(
                "summary" to "Fallback: la IA no devolvió JSON válido.",
                "kpis" to emptyList<Map<String, String>>(),
                "sections" to listOf(mapOf("title" to "Error", "content" to ex.message)),
            )
        }
    }
}