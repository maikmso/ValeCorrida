package com.miqueias.valecorrida

data class Oferta(
    val valor: Double,   // R$
    val km: Double,      // busca + viagem
    val minutos: Int     // busca + viagem (0 = não encontrado)
)

/**
 * Transforma o texto lido da tela em uma Oferta.
 * Exemplos de texto que ele entende:
 *   "R$ 18,50 | 4 min (1,2 km) de distância | Viagem de 15 min (8,3 km)"
 *   "R$ 15,90 | 800 m | 1h 10min | 32,4 km"
 */
object OfertaParser {

    private const val LETRA = "a-zA-ZÀ-ÿ"

    // R$ 1.234,56 | R$ 12,50 | R$12.50 | R$ 12
    private val rValor = Regex("""R\$\s*(\d{1,3}(?:\.\d{3})+,\d{1,2}|\d+(?:[.,]\d{1,2})?)""")

    // 8,3 km | 800 m  (não pega "min" nem "metros")
    private val rDistancia = Regex("""(\d+(?:[.,]\d+)?)\s*(km|m)(?![$LETRA])""", RegexOption.IGNORE_CASE)

    private val rHoraMin = Regex("""(\d+)\s*h\s*(\d+)\s*min""", RegexOption.IGNORE_CASE)
    private val rHora = Regex("""(\d+)\s*h(?![$LETRA])""", RegexOption.IGNORE_CASE)
    private val rMin = Regex("""(\d+)\s*min""", RegexOption.IGNORE_CASE)

    fun extrair(textoBruto: String): Oferta? {
        val texto = textoBruto
            .replace('\u00A0', ' ')
            .replace('\u202F', ' ')

        // --- Distância ---
        val distancias = rDistancia.findAll(texto).toList()
        if (distancias.isEmpty()) return null
        var km = 0.0
        for (m in distancias) {
            val n = numero(m.groupValues[1]) ?: continue
            km += if (m.groupValues[2].equals("km", ignoreCase = true)) n else n / 1000.0
        }
        if (km <= 0.0 || km > 500.0) return null

        // --- Valor ---
        // Ignora valores que não são o preço da corrida:
        //   "+R$ 8,00 incluído" (bônus, tem "+" antes) e "R$1,66/km" (tem "/" depois).
        // Fica com o primeiro que sobrar antes da primeira distância.
        val valores = rValor.findAll(texto).toList()
        if (valores.isEmpty()) return null
        val primeiraDist = distancias.first().range.first
        val candidatos = valores.filter { m ->
            val antes = texto.substring(0, m.range.first).trimEnd()
            val depois = texto.substring(m.range.last + 1).trimStart()
            !antes.endsWith("+") && !depois.startsWith("/")
        }
        val escolhido = candidatos.firstOrNull { it.range.first < primeiraDist }
            ?: candidatos.firstOrNull()
            ?: valores.first()
        val valor = numero(escolhido.groupValues[1]) ?: return null
        if (valor <= 0.0) return null

        // --- Tempo ---
        var resto = texto
        var minutos = 0
        rHoraMin.findAll(resto).forEach {
            minutos += it.groupValues[1].toInt() * 60 + it.groupValues[2].toInt()
        }
        resto = rHoraMin.replace(resto, " ")
        rHora.findAll(resto).forEach { minutos += it.groupValues[1].toInt() * 60 }
        resto = rHora.replace(resto, " ")
        rMin.findAll(resto).forEach { minutos += it.groupValues[1].toInt() }

        return Oferta(valor, km, minutos)
    }

    /** "1.234,56" -> 1234.56 | "12,5" -> 12.5 | "12.50" -> 12.5 */
    private fun numero(s: String): Double? {
        val limpo = if (s.contains(',')) s.replace(".", "").replace(',', '.') else s
        return limpo.toDoubleOrNull()
    }
}
