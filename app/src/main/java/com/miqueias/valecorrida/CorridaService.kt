package com.miqueias.valecorrida

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Lê a tela do Uber/99, encontra a oferta e mostra o balão.
 * Não clica em nada: só lê e mostra o cálculo.
 */
class CorridaService : AccessibilityService() {

    companion object {
        /** Referência para o botão "Testar balão" da tela principal. */
        var instancia: CorridaService? = null
            private set
    }

    private lateinit var prefs: Prefs
    private lateinit var balao: Balao
    private val handler = Handler(Looper.getMainLooper())
    private var pendente: Runnable? = null
    private var ultimaAssinatura = ""
    /** Texto que chega junto com os eventos (às vezes é o único jeito de ler o card flutuante). */
    private val textosEvento = LinkedHashMap<String, LinkedHashSet<String>>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = Prefs(this)
        // TYPE_ACCESSIBILITY_OVERLAY: janela por cima de tudo, desenhada pelo próprio serviço
        balao = Balao(this, prefs, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)
        instancia = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || instancia == null) return
        val pkg = event.packageName?.toString()

        // Diagnóstico: guarda o último app aberto (ajuda a descobrir o pacote da 99)
        if (pkg != null && event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            pkg != packageName && pkg != "com.android.systemui"
        ) {
            prefs.ultimoAppVisto = pkg
        }

        // A oferta do Uber/99 costuma abrir numa janela flutuante. Nesse caso o evento
        // pode vir sem pacote (TYPE_WINDOWS_CHANGED), então analisamos todas as janelas.
        val interessa = (pkg != null && monitorado(pkg)) ||
            event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED
        if (!interessa) return

        if (pkg != null && monitorado(pkg)) {
            val set = textosEvento.getOrPut(pkg) { LinkedHashSet() }
            event.text?.forEach { t -> t?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { set.add(it) } }
            event.contentDescription?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { set.add(it) }
        }

        // Espera a tela "assentar" antes de ler (evita ler 20x por segundo)
        pendente?.let { handler.removeCallbacks(it) }
        val r = Runnable { analisar() }
        pendente = r
        handler.postDelayed(r, 350)
    }

    /** Pacotes da lista + qualquer app com "uber" ou "99" no nome do pacote. */
    private fun monitorado(pkg: String): Boolean {
        if (pkg == packageName) return false
        if (pkg in prefs.listaPacotes()) return true
        val p = pkg.lowercase()
        return p.contains("uber") || p.contains("99")
    }

    /** Lê todas as janelas visíveis do Uber/99 e mostra o balão da primeira oferta encontrada. */
    private fun analisar() {
        // Junta o texto por app (a lista de janelas vem da que está mais na frente para trás)
        val porApp = LinkedHashMap<String, LinkedHashSet<String>>()
        try {
            for (janela in windows) {
                val raiz = janela.root ?: continue
                val pkg = raiz.packageName?.toString() ?: continue
                if (!monitorado(pkg)) continue
                coletar(raiz, porApp.getOrPut(pkg) { LinkedHashSet() }, 0)
            }
        } catch (_: Exception) { }

        if (porApp.isEmpty()) {
            val raiz = rootInActiveWindow
            val pkg = raiz?.packageName?.toString()
            if (raiz != null && pkg != null && monitorado(pkg)) {
                coletar(raiz, porApp.getOrPut(pkg) { LinkedHashSet() }, 0)
            }
        }
        // Junta o texto que veio nos eventos
        for ((pkg, set) in textosEvento) {
            if (set.isNotEmpty()) porApp.getOrPut(pkg) { LinkedHashSet() }.addAll(set)
        }
        textosEvento.clear()
        if (porApp.isEmpty()) return

        for ((pkg, textos) in porApp) {
            if (textos.isEmpty()) continue
            val texto = removerAgregados(textos.toList()).joinToString(" | ")

            if (texto.contains("R$") || texto.contains("km", ignoreCase = true)) {
                prefs.ultimoTexto = "[$pkg]\n$texto"
            }

            val oferta = OfertaParser.extrair(texto) ?: continue
            val assinatura = "$pkg|${oferta.valor}|${oferta.km}|${oferta.minutos}"
            if (assinatura == ultimaAssinatura && balao.visivel) return
            ultimaAssinatura = assinatura

            balao.mostrar(Calculadora.calcular(oferta, prefs))
            return
        }
    }

    private fun coletar(no: AccessibilityNodeInfo?, saida: MutableSet<String>, profundidade: Int) {
        if (no == null || profundidade > 50) return
        val t = no.text?.toString()?.trim()
        if (!t.isNullOrEmpty()) {
            saida.add(t)
        } else if (no.childCount == 0) {
            val d = no.contentDescription?.toString()?.trim()
            if (!d.isNullOrEmpty()) saida.add(d)
        }
        for (i in 0 until no.childCount) {
            coletar(no.getChild(i), saida, profundidade + 1)
        }
    }

    /**
     * Alguns apps repetem o texto dos filhos num elemento "pai" (ex.: "R$ 12,50 4 min (1,2 km)").
     * Isso faria somar km/min em dobro, então removemos o texto que contém 2+ outros textos.
     */
    private fun removerAgregados(lista: List<String>): List<String> =
        lista.filter { s ->
            lista.count { t -> t != s && t.length >= 3 && s.contains(t) } < 2
        }

    fun mostrarTeste() {
        balao.mostrar(Calculadora.calcular(Oferta(18.50, 9.5, 19), prefs))
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        limpar()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        limpar()
        super.onDestroy()
    }

    private fun limpar() {
        pendente?.let { handler.removeCallbacks(it) }
        if (::balao.isInitialized) balao.esconder()
        instancia = null
    }
}
