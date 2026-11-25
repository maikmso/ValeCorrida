package com.miqueias.valecorrida

import android.content.Context

/** Configurações do motorista, salvas no celular. */
class Prefs(context: Context) {

    private val sp = context.getSharedPreferences("config", Context.MODE_PRIVATE)

    var precoLitro: Double
        get() = getDouble("precoLitro", 6.29)
        set(v) = putDouble("precoLitro", v)

    var kmPorLitro: Double
        get() = getDouble("kmPorLitro", 10.0)
        set(v) = putDouble("kmPorLitro", v)

    var metaPorKm: Double
        get() = getDouble("metaPorKm", 1.80)
        set(v) = putDouble("metaPorKm", v)

    var metaPorHora: Double
        get() = getDouble("metaPorHora", 30.0)
        set(v) = putDouble("metaPorHora", v)

    var segundosBalao: Int
        get() = sp.getInt("segundosBalao", 15)
        set(v) = sp.edit().putInt("segundosBalao", v).apply()

    var pacotes: String
        get() = sp.getString("pacotes", PACOTES_PADRAO) ?: PACOTES_PADRAO
        set(v) = sp.edit().putString("pacotes", v).apply()

    // Posição do balão (o motorista pode arrastar)
    var balaoX: Int
        get() = sp.getInt("balaoX", 0)
        set(v) = sp.edit().putInt("balaoX", v).apply()

    var balaoY: Int
        get() = sp.getInt("balaoY", 200)
        set(v) = sp.edit().putInt("balaoY", v).apply()

    // Diagnóstico
    var ultimoTexto: String
        get() = sp.getString("ultimoTexto", "") ?: ""
        set(v) = sp.edit().putString("ultimoTexto", v).apply()

    var ultimoAppVisto: String
        get() = sp.getString("ultimoAppVisto", "") ?: ""
        set(v) = sp.edit().putString("ultimoAppVisto", v).apply()

    fun listaPacotes(): Set<String> =
        pacotes.split(',', ';', '\n', ' ')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

    private fun getDouble(chave: String, padrao: Double): Double =
        sp.getString(chave, null)?.toDoubleOrNull() ?: padrao

    private fun putDouble(chave: String, valor: Double) =
        sp.edit().putString(chave, valor.toString()).apply()

    companion object {
        const val PACOTES_PADRAO = "com.ubercab.driver, com.app99.driver, com.taxis99"
    }
}
