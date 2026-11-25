package com.miqueias.valecorrida

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var prefs: Prefs

    private lateinit var txtStatusGeral: TextView
    private lateinit var txtSobreposicao: TextView
    private lateinit var txtAcessibilidade: TextView

    private lateinit var edtPrecoLitro: EditText
    private lateinit var edtKmPorLitro: EditText
    private lateinit var edtMetaKm: EditText
    private lateinit var edtMetaHora: EditText
    private lateinit var edtSegundos: EditText
    private lateinit var edtPacotes: EditText

    private lateinit var layoutDiagnostico: LinearLayout
    private lateinit var txtToggleDiag: TextView
    private lateinit var txtUltimoApp: TextView
    private lateinit var txtUltimoTexto: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = Prefs(this)

        txtStatusGeral = findViewById(R.id.txtStatusGeral)
        txtSobreposicao = findViewById(R.id.txtSobreposicao)
        txtAcessibilidade = findViewById(R.id.txtAcessibilidade)
        edtPrecoLitro = findViewById(R.id.edtPrecoLitro)
        edtKmPorLitro = findViewById(R.id.edtKmPorLitro)
        edtMetaKm = findViewById(R.id.edtMetaKm)
        edtMetaHora = findViewById(R.id.edtMetaHora)
        edtSegundos = findViewById(R.id.edtSegundos)
        edtPacotes = findViewById(R.id.edtPacotes)
        layoutDiagnostico = findViewById(R.id.layoutDiagnostico)
        txtToggleDiag = findViewById(R.id.txtToggleDiag)
        txtUltimoApp = findViewById(R.id.txtUltimoApp)
        txtUltimoTexto = findViewById(R.id.txtUltimoTexto)

        findViewById<View>(R.id.rowSobreposicao).setOnClickListener {
            abrir(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }

        findViewById<View>(R.id.rowAcessibilidade).setOnClickListener {
            abrir(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            if (!acessibilidadeAtiva()) toast("Vá em “Apps instalados” → Vale a Corrida → ative")
        }

        findViewById<View>(R.id.rowInfoApp).setOnClickListener {
            abrir(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }

        findViewById<View>(R.id.btnSalvar).setOnClickListener { salvar() }

        findViewById<View>(R.id.btnTestar).setOnClickListener {
            val servico = CorridaService.instancia
            if (servico == null) toast("Ative a leitura de tela primeiro")
            else servico.mostrarTeste()
        }

        txtToggleDiag.setOnClickListener {
            val abrir = layoutDiagnostico.visibility != View.VISIBLE
            layoutDiagnostico.visibility = if (abrir) View.VISIBLE else View.GONE
            txtToggleDiag.text = if (abrir) "Diagnóstico ▴" else "Diagnóstico ▾"
        }

        findViewById<View>(R.id.btnCopiar).setOnClickListener {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("texto lido", prefs.ultimoTexto))
            toast("Copiado")
        }

        carregarCampos()
    }

    override fun onResume() {
        super.onResume()
        atualizarStatus()
        atualizarDiagnostico()
    }

    private fun atualizarStatus() {
        val sobreposicao = Settings.canDrawOverlays(this)
        val acessibilidade = acessibilidadeAtiva()

        // Permissões: verde "Ativo" ou azul "Ativar"
        chip(txtSobreposicao, sobreposicao, "✓ Ativo", "Ativar",
            R.drawable.bg_chip_ativar, R.color.azul)
        chip(txtAcessibilidade, acessibilidade, "✓ Ativo", "Ativar",
            R.drawable.bg_chip_ativar, R.color.azul)
        // Status do topo: verde "Ligado" ou amarelo "Desligado"
        chip(txtStatusGeral, acessibilidade, "● Ligado", "● Desligado",
            R.drawable.bg_chip_off, R.color.ambar)
    }

    private fun chip(
        tv: TextView, ok: Boolean, textoOk: String, textoOff: String,
        fundoOff: Int, corOff: Int
    ) {
        tv.text = if (ok) textoOk else textoOff
        tv.setBackgroundResource(if (ok) R.drawable.bg_chip_ok else fundoOff)
        tv.setTextColor(getColor(if (ok) R.color.verde else corOff))
    }

    private fun acessibilidadeAtiva(): Boolean {
        val esperado = ComponentName(this, CorridaService::class.java)
        val ativos = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return ativos.split(':').any { ComponentName.unflattenFromString(it) == esperado }
    }

    private fun carregarCampos() {
        edtPrecoLitro.setText(Fmt.num(prefs.precoLitro, 2))
        edtKmPorLitro.setText(Fmt.num(prefs.kmPorLitro, 1))
        edtMetaKm.setText(Fmt.num(prefs.metaPorKm, 2))
        edtMetaHora.setText(Fmt.num(prefs.metaPorHora, 2))
        edtSegundos.setText(prefs.segundosBalao.toString())
        edtPacotes.setText(prefs.pacotes)
    }

    private fun salvar() {
        val preco = numero(edtPrecoLitro)
        val consumo = numero(edtKmPorLitro)
        val metaKm = numero(edtMetaKm)
        val metaHora = numero(edtMetaHora)
        val segundos = edtSegundos.text.toString().trim().toIntOrNull()

        if (preco == null || consumo == null || metaKm == null || metaHora == null || segundos == null || consumo <= 0) {
            toast("Confira os números (use vírgula ou ponto)")
            return
        }

        prefs.precoLitro = preco
        prefs.kmPorLitro = consumo
        prefs.metaPorKm = metaKm
        prefs.metaPorHora = metaHora
        prefs.segundosBalao = segundos.coerceIn(3, 120)
        prefs.pacotes = edtPacotes.text.toString().ifBlank { Prefs.PACOTES_PADRAO }
        carregarCampos()
        esconderTeclado()
        toast("Salvo ✅")
    }

    private fun atualizarDiagnostico() {
        val app = prefs.ultimoAppVisto.ifBlank { "nenhum ainda" }
        txtUltimoApp.text = "Último app aberto: $app\n(abra a 99 e volte aqui para ver o pacote dela)"
        txtUltimoTexto.text = prefs.ultimoTexto.ifBlank { "Nada lido ainda. Fique online no Uber/99 e espere uma oferta." }
    }

    private fun esconderTeclado() {
        currentFocus?.let {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0)
            it.clearFocus()
        }
    }

    private fun numero(e: EditText): Double? =
        e.text.toString().trim().replace(',', '.').toDoubleOrNull()

    private fun abrir(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
