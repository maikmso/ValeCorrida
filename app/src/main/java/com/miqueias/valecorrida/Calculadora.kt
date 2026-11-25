package com.miqueias.valecorrida

import java.util.Locale

enum class Nivel { BOA, MEDIA, RUIM }

data class Resultado(
    val oferta: Oferta,
    val porKm: Double,
    val porHora: Double?,        // null quando o tempo não foi encontrado
    val custoCombustivel: Double,
    val lucro: Double,
    val nivel: Nivel
)

object Calculadora {

    fun calcular(o: Oferta, p: Prefs): Resultado {
        val porKm = o.valor / o.km
        val porHora = if (o.minutos > 0) o.valor / (o.minutos / 60.0) else null
        val custo = if (p.kmPorLitro > 0) o.km / p.kmPorLitro * p.precoLitro else 0.0
        val lucro = o.valor - custo

        val okKm = porKm >= p.metaPorKm
        val nivel = if (porHora == null) {
            if (okKm) Nivel.BOA else Nivel.RUIM
        } else {
            val okHora = porHora >= p.metaPorHora
            when {
                okKm && okHora -> Nivel.BOA
                okKm || okHora -> Nivel.MEDIA
                else -> Nivel.RUIM
            }
        }
        return Resultado(o, porKm, porHora, custo, lucro, nivel)
    }
}

object Fmt {
    private val br = Locale("pt", "BR")
    fun reais(v: Double) = "R$ " + "%.2f".format(br, v)
    fun num(v: Double, casas: Int = 1) = "%.${casas}f".format(br, v)
}
