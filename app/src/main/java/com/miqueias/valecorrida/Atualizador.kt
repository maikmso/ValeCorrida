package com.miqueias.valecorrida

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class Versao(val codigo: Int, val urlApk: String)

/**
 * Atualização dentro do app:
 * 1. Consulta a última Release do GitHub (tag "v<número>").
 * 2. Se o número for maior que o versionCode instalado, oferece atualizar.
 * 3. Baixa o APK e entrega para o instalador do Android (o usuário confirma).
 */
object Atualizador {

    private const val API = "https://api.github.com/repos/maikmso/ValeCorrida/releases/latest"
    const val ACAO_STATUS = "com.miqueias.valecorrida.STATUS_INSTALACAO"

    fun versaoAtual(a: Activity): Int {
        val info = a.packageManager.getPackageInfo(a.packageName, 0)
        return if (Build.VERSION.SDK_INT >= 28) {
            info.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            info.versionCode
        }
    }

    /** Chamar FORA da thread principal. Retorna null se não conseguir consultar. */
    fun buscarUltima(): Versao? {
        return try {
            val c = URL(API).openConnection() as HttpURLConnection
            c.setRequestProperty("Accept", "application/vnd.github+json")
            c.setRequestProperty("User-Agent", "ValeCorrida")
            c.connectTimeout = 8000
            c.readTimeout = 8000
            if (c.responseCode != 200) return null

            val json = JSONObject(c.inputStream.bufferedReader().use { it.readText() })
            val codigo = json.getString("tag_name").removePrefix("v").toIntOrNull() ?: return null

            val assets = json.getJSONArray("assets")
            var url: String? = null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.getString("name").endsWith(".apk")) {
                    url = asset.getString("browser_download_url")
                    break
                }
            }
            if (url == null) null else Versao(codigo, url)
        } catch (e: Exception) {
            null
        }
    }

    /** Chamar FORA da thread principal. Baixa o APK e abre a confirmação do Android. */
    fun baixarEInstalar(a: Activity, v: Versao, progresso: (Int) -> Unit) {
        val installer = a.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        params.setAppPackageName(a.packageName)
        val idSessao = installer.createSession(params)

        installer.openSession(idSessao).use { sessao ->
            val c = URL(v.urlApk).openConnection() as HttpURLConnection
            c.setRequestProperty("User-Agent", "ValeCorrida")
            c.instanceFollowRedirects = true
            c.connectTimeout = 15000
            c.readTimeout = 30000
            val total = c.contentLengthLong

            c.inputStream.use { entrada ->
                sessao.openWrite("ValeCorrida.apk", 0, if (total > 0) total else -1).use { saida ->
                    val buffer = ByteArray(64 * 1024)
                    var lido = 0L
                    while (true) {
                        val n = entrada.read(buffer)
                        if (n < 0) break
                        saida.write(buffer, 0, n)
                        lido += n
                        if (total > 0) progresso((lido * 100 / total).toInt())
                    }
                    sessao.fsync(saida)
                }
            }

            // O resultado volta para a MainActivity (onNewIntent)
            val intent = Intent(a, MainActivity::class.java)
                .setAction(ACAO_STATUS)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
            val pi = PendingIntent.getActivity(a, idSessao, intent, flags)
            sessao.commit(pi.intentSender)
        }
    }
}
