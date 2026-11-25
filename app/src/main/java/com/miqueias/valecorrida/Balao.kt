package com.miqueias.valecorrida

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

/**
 * Balão que aparece por cima do Uber/99.
 * Toque = fecha. Arrastar = muda de lugar (a posição fica salva).
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

    fun mostrar(r: Resultado) {
        esconder()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            tipoJanela,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = prefs.balaoX
            y = prefs.balaoY
        }

        val v = criarView(r)
        ativarArrastar(v, params)

        try {
            wm.addView(v, params)
            view = v
        } catch (e: Exception) {
            view = null
            return
        }

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

    private fun criarView(r: Resultado): View {
        val cor = when (r.nivel) {
            Nivel.BOA -> Color.parseColor("#2E7D32")
            Nivel.MEDIA -> Color.parseColor("#E08A00")
            Nivel.RUIM -> Color.parseColor("#C62828")
        }
        val titulo = when (r.nivel) {
            Nivel.BOA -> "✅ VALE A PENA"
            Nivel.MEDIA -> "⚠️ MAIS OU MENOS"
            Nivel.RUIM -> "❌ NÃO VALE"
        }

        val caixa = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(18), dp(12), dp(18), dp(12))
            background = GradientDrawable().apply {
                setColor(cor)
                cornerRadius = dp(18).toFloat()
                setStroke(dp(2), Color.WHITE)
            }
            elevation = dp(8).toFloat()
        }

        val o = r.oferta
        val porHora = r.porHora?.let { Fmt.reais(it) + "/h" } ?: "—/h"
        val tempo = if (o.minutos > 0) "${o.minutos} min" else "? min"

        caixa.addView(texto(titulo, 13f, negrito = true))
        caixa.addView(texto(Fmt.reais(r.porKm) + "/km", 28f, negrito = true))
        caixa.addView(texto(porHora, 18f, negrito = true))
        caixa.addView(texto("${Fmt.reais(o.valor)} • ${Fmt.num(o.km)} km • $tempo", 14f))
        caixa.addView(texto("Combustível ${Fmt.reais(r.custoCombustivel)} → Lucro ${Fmt.reais(r.lucro)}", 14f))
        return caixa
    }

    private fun texto(s: String, sp: Float, negrito: Boolean = false) = TextView(context).apply {
        text = s
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        gravity = Gravity.CENTER
        if (negrito) setTypeface(typeface, Typeface.BOLD)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun ativarArrastar(v: View, params: WindowManager.LayoutParams) {
        var inicioX = 0
        var inicioY = 0
        var toqueX = 0f
        var toqueY = 0f
        var moveu = false

        v.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    inicioX = params.x; inicioY = params.y
                    toqueX = ev.rawX; toqueY = ev.rawY
                    moveu = false
                    handler.removeCallbacks(esconderRunnable)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = ev.rawX - toqueX
                    val dy = ev.rawY - toqueY
                    if (abs(dx) > dp(6) || abs(dy) > dp(6)) moveu = true
                    if (moveu) {
                        params.x = inicioX + dx.toInt()
                        params.y = inicioY + dy.toInt()
                        try { wm.updateViewLayout(v, params) } catch (_: Exception) { }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (moveu) {
                        prefs.balaoX = params.x
                        prefs.balaoY = params.y
                        handler.postDelayed(esconderRunnable, prefs.segundosBalao.coerceAtLeast(3) * 1000L)
                    } else {
                        esconder()
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
