package com.miqueias.valecorrida

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

/**
 * Banner no topo da tela, no estilo de uma notificação chegando,
 * por cima do Uber/99. A oferta do app continua visível embaixo.
 *
 * Toque = fecha. Arrastar para cima = fecha. Arrastar para baixo = muda a altura (fica salva).
 */
class Balao(
    private val context: Context,
    private val prefs: Prefs,
    private val tipoJanela: Int
) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private val esconderRunnable = Runnable { esconder() }
    private var view: View? = null

    val visivel: Boolean get() = view != null

    private val VERDE = Color.parseColor("#16A34A")
    private val AMARELO = Color.parseColor("#D97706")
    private val VERMELHO = Color.parseColor("#DC2626")
    private val CINZA = Color.parseColor("#9CA3AF")
    private val TEXTO = Color.parseColor("#0F172A")
    private val TEXTO2 = Color.parseColor("#475569")

    /** @param app nome de onde veio a oferta ("Uber", "99"...) */
    fun mostrar(r: Resultado, app: String) {
        esconder()

        val largura = context.resources.displayMetrics.widthPixels - dp(16)
        val params = WindowManager.LayoutParams(
            largura,
            WindowManager.LayoutParams.WRAP_CONTENT,
            tipoJanela,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = prefs.bannerY.coerceAtLeast(dp(6))
        }

        val v = criarView(r, app)
        ativarGestos(v, params)

        try {
            wm.addView(v, params)
            view = v
        } catch (e: Exception) {
            view = null
            return
        }

        // Entra deslizando de cima, como notificação
        v.translationY = -dp(120).toFloat()
        v.alpha = 0f
        v.animate().translationY(0f).alpha(1f).setDuration(220).start()

        handler.removeCallbacks(esconderRunnable)
        handler.postDelayed(esconderRunnable, prefs.segundosBalao.coerceAtLeast(3) * 1000L)
    }

    fun esconder() {
        handler.removeCallbacks(esconderRunnable)
        view?.let {
            try { wm.removeView(it) } catch (_: Exception) { }
        }
        view = null
    }

    private fun criarView(r: Resultado, app: String): View {
        val o = r.oferta
        val okKm = r.porKm >= prefs.metaPorKm
        val okHora = r.porHora?.let { it >= prefs.metaPorHora }
        val corGeral = when (r.nivel) {
            Nivel.BOA -> VERDE
            Nivel.MEDIA -> AMARELO
            Nivel.RUIM -> VERMELHO
        }
        val selo = when (r.nivel) {
            Nivel.BOA -> "VALE A PENA"
            Nivel.MEDIA -> "MAIS OU MENOS"
            Nivel.RUIM -> "NÃO VALE"
        }

        // ---- Card da "notificação" ----
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F7FFFFFF"))
                cornerRadius = dp(20).toFloat()
                setStroke(dp(1), Color.parseColor("#E5E7EB"))
            }
            elevation = dp(10).toFloat()
        }

        // Ícone do app (fundo na cor do resultado)
        val icone = ImageView(context).apply {
            setImageResource(R.drawable.ic_launcher)
            background = GradientDrawable().apply {
                setColor(corGeral)
                cornerRadius = dp(10).toFloat()
            }
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        card.addView(icone, LinearLayout.LayoutParams(dp(40), dp(40)).apply {
            topMargin = dp(2)
            marginEnd = dp(12)
        })

        val coluna = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        card.addView(coluna, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        // Linha 0: VALE A CORRIDA · Uber            selo
        val topo = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        topo.addView(texto("VALE A CORRIDA · $app", 11f, TEXTO2, negrito = true).apply {
            letterSpacing = 0.05f
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        topo.addView(texto(selo, 10f, Color.WHITE, negrito = true).apply {
            letterSpacing = 0.06f
            setPadding(dp(8), dp(2), dp(8), dp(2))
            background = GradientDrawable().apply {
                setColor(corGeral)
                cornerRadius = dp(100).toFloat()
            }
        })
        coluna.addView(topo)

        // Linha 1: ● R$ 17,38   ● R$ 10,22/km   ● R$ 104/h
        val linha1 = SpannableStringBuilder()
        bolinha(linha1, corGeral); negrito(linha1, Fmt.reais(o.valor)); linha1.append("    ")
        bolinha(linha1, if (okKm) VERDE else VERMELHO); negrito(linha1, Fmt.num(r.porKm, 2) + "/km"); linha1.append("    ")
        bolinha(linha1, when (okHora) { true -> VERDE; false -> VERMELHO; null -> CINZA })
        negrito(linha1, r.porHora?.let { Fmt.num(it, 0) + "/h" } ?: "—/h")
        coluna.addView(texto("", 16f, TEXTO).apply {
            text = linha1
            setPadding(0, dp(4), 0, 0)
        })

        // Linha 2: Lucro: R$ 16,00 (93%) · ⛽ R$ 1,38
        val pct = if (o.valor > 0) (r.lucro / o.valor * 100).toInt() else 0
        coluna.addView(texto(
            "Lucro: ${Fmt.reais(r.lucro)} ($pct%)   ⛽ ${Fmt.reais(r.custoCombustivel)}",
            14f, TEXTO, negrito = true
        ).apply { setPadding(0, dp(2), 0, 0) })

        // Linha 3: Uber · 34 min (22,3 km)
        val tempo = if (o.minutos > 0) "${o.minutos} min" else "? min"
        coluna.addView(texto("$app · $tempo (${Fmt.num(o.km)} km)", 13f, TEXTO2).apply {
            setPadding(0, dp(2), 0, 0)
        })

        return card
    }

    private fun bolinha(sb: SpannableStringBuilder, cor: Int) {
        val ini = sb.length
        sb.append("● ")
        sb.setSpan(ForegroundColorSpan(cor), ini, ini + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun negrito(sb: SpannableStringBuilder, s: String) {
        val ini = sb.length
        sb.append(s)
        sb.setSpan(StyleSpan(Typeface.BOLD), ini, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun texto(s: String, sp: Float, cor: Int, negrito: Boolean = false) = TextView(context).apply {
        text = s
        setTextColor(cor)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        if (negrito) setTypeface(typeface, Typeface.BOLD)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun ativarGestos(v: View, params: WindowManager.LayoutParams) {
        var inicioY = 0
        var toqueY = 0f
        var moveu = false

        v.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    inicioY = params.y
                    toqueY = ev.rawY
                    moveu = false
                    handler.removeCallbacks(esconderRunnable)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dy = ev.rawY - toqueY
                    if (abs(dy) > dp(6)) moveu = true
                    if (moveu) {
                        params.y = (inicioY + dy.toInt()).coerceAtLeast(0)
                        try { wm.updateViewLayout(v, params) } catch (_: Exception) { }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val dy = ev.rawY - toqueY
                    when {
                        !moveu -> esconder()                       // toque: fecha
                        dy < -dp(40) -> esconder()                  // arrastou pra cima: fecha
                        else -> {                                   // arrastou pra baixo: salva altura
                            prefs.bannerY = params.y
                            handler.postDelayed(esconderRunnable, prefs.segundosBalao.coerceAtLeast(3) * 1000L)
                        }
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun dp(v: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), context.resources.displayMetrics
        ).toInt()
}
