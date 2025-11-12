package com.tuorg.fleetcare.service


import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class GroqService(
    @Value("\${groq.api.key}") private val apiKey: String?,
    @Value("\${groq.model}") private val model: String,
    private val mapper: ObjectMapper
) {

    private val client = WebClient.builder()
        .baseUrl("https://api.groq.com/openai/v1/chat/completions")
        .defaultHeader("Authorization", "Bearer ${apiKey ?: ""}")
        .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        .build()

    fun generateJson(prompt: String): Map<String, Any?> {

        if (apiKey.isNullOrBlank()) {
            return mapOf(
                "summary" to "Fallback local: sin API KEY",
                "kpis" to listOf(mapOf("name" to "Ejemplo", "value" to 0)),
                "sections" to listOf(mapOf("title" to "Nota", "content" to "Configure GROQ_API_KEY")),
                "dataHash" to "no-key"
            )
        }

        val payload = mapOf(
            "model" to model,
            "temperature" to 0.2,
            "messages" to listOf(
                mapOf("role" to "system",
                    "content" to """
                        Responde estrictamente SOLO un JSON válido.
                        Sin markdown, sin ```json, sin texto adicional.
                        Si no puedes generar JSON, responde {"error":"invalid-json"}.
                    """.trimIndent()
                ),
                mapOf("role" to "user", "content" to prompt)
            )
        )

        val resp = client.post()
            .bodyValue(payload)
            .retrieve()
            .bodyToMono(Map::class.java)
            .block() ?: return fallback("Respuesta nula de Groq")

        // ========= EXTRAER EL CONTENIDO CORRECTO ===========
        val choices = resp["choices"] as? List<*> ?: return fallback("choices vacío")
        val msg = choices.firstOrNull() as? Map<*, *> ?: return fallback("choices[0] null")
        val message = msg["message"] as? Map<*, *> ?: return fallback("message null")
        val text = message["content"]?.toString() ?: "{}"

        return try {
            @Suppress("UNCHECKED_CAST")
            mapper.readValue(text, Map::class.java) as Map<String, Any?>
        } catch (ex: Exception) {
            fallback("Groq devolvió texto NO JSON: $text")
        }
    }

    private fun fallback(reason: String): Map<String, Any?> {
        return mapOf(
            "summary" to "Fallback: $reason",
            "kpis" to emptyList<Map<String, Any?>>(),
            "sections" to listOf(
                mapOf("title" to "Error", "content" to reason)
            )
        )
    }
}