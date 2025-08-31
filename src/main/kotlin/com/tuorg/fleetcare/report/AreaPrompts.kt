package com.tuorg.fleetcare.report

import com.tuorg.fleetcare.user.domain.Area

object AreaPrompts {
    fun baseSchema() = """
    Devuelve SOLO JSON válido con estructura:
    {
      "summary": string,
      "generatedAt": string, 
      "status": string, 
      "kpis": [{"name": string, "value": number}],
      "sections": [{"title": string, "content": string}],
      "dataHash": string
    }
  """.trimIndent()

    fun by(area: Area) = when (area) {
        Area.OPERATIONS -> """
      Eres analista de Operaciones. ${baseSchema()}
      Enfócate en: disponibilidad de flota, impacto a programación y tiempos estimados de retorno.
    """.trimIndent()
        Area.FINANCE -> """
      Eres analista de Finanzas. ${baseSchema()}
      Enfócate en: costos estimados, OPEX/CAPEX, riesgos de sobrecosto y optimizaciones.
    """.trimIndent()
        Area.MAINTENANCE -> """
      Eres analista de Mantenimiento. ${baseSchema()}
      Enfócate en: tipo de mantenimiento, repuestos críticos, mano de obra y plan de taller.
    """.trimIndent()
        Area.COMMERCIAL -> """
      Eres analista Comercial. ${baseSchema()}
      Enfócate en: impacto al cliente, rutas afectadas, SLAs y mitigaciones.
    """.trimIndent()
    }
}