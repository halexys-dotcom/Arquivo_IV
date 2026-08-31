package pt.haconnect.arquivoiv.domain.usecase

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class UpdateInfo(
    val tagName: String = "",
    val versionName: String = "",
    val releaseNotes: String = "",
    val apkUrl: String = "",
    val apkSizeBytes: Long = 0L,
    val isUpdateAvailable: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String? = null
)

@Singleton
class UpdateUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val GITHUB_RELEASES_API = "https://api.github.com/repos/halexys-dotcom/Arquivo_IV/releases/latest"
        private const val GITHUB_RELEASES_URL = "https://github.com/halexys-dotcom/Arquivo_IV/releases"
        private const val TIMEOUT_MS = 8000
    }

    /**
     * Verifica na API do GitHub se existe uma versão mais recente do que a instalada.
     */
    suspend fun verificarAtualizacao(currentVersionName: String): UpdateInfo = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_RELEASES_API)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "ArquivoIV-AndroidApp")
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.w("UpdateUseCase", "Resposta da API GitHub: ${connection.responseCode}")
                return@withContext UpdateInfo(isUpdateAvailable = false, isError = false)
            }

            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseText)

            val tagName = json.optString("tag_name", "")
            val body = json.optString("body", "")

            var apkUrl = ""
            var apkSizeBytes = 0L

            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url", "")
                        apkSizeBytes = asset.optLong("size", 0L)
                        break
                    }
                }
            }

            val cleanLatestVersion = extrairVersaoSemantica(tagName)
            val cleanCurrentVersion = extrairVersaoSemantica(currentVersionName)

            val isNewer = compararVersoes(cleanLatestVersion, cleanCurrentVersion) > 0

            UpdateInfo(
                tagName = tagName,
                versionName = cleanLatestVersion,
                releaseNotes = body,
                apkUrl = apkUrl,
                apkSizeBytes = apkSizeBytes,
                isUpdateAvailable = isNewer && apkUrl.isNotBlank()
            )
        } catch (e: Exception) {
            Log.w("UpdateUseCase", "Erro ao verificar atualizações (sem rede ou offline): ${e.message}")
            UpdateInfo(isUpdateAvailable = false, isError = true, errorMessage = "Não foi possível verificar atualizações.")
        }
    }

    /**
     * Descarrega o ficheiro APK da release com callback de progresso.
     */
    suspend fun descarregarApk(
        apkUrl: String,
        expectedSizeBytes: Long,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val destFile = File(context.cacheDir, "update.apk")
            if (destFile.exists()) destFile.delete()

            var currentUrl = apkUrl
            var connection: HttpURLConnection
            var redirects = 0

            while (true) {
                val url = URL(currentUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 15000
                    setRequestProperty("User-Agent", "ArquivoIV-AndroidApp")
                }
                val code = connection.responseCode
                if (code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP || code == HttpURLConnection.HTTP_SEE_OTHER) {
                    currentUrl = connection.getHeaderField("Location")
                    redirects++
                    if (redirects > 5) throw IllegalStateException("Muitos redirecionamentos ao descarregar o APK.")
                } else {
                    break
                }
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("Erro ao descarregar APK: Código HTTP ${connection.responseCode}")
            }

            val contentLength = if (connection.contentLengthLong > 0) connection.contentLengthLong else expectedSizeBytes
            var downloadedBytes = 0L

            connection.inputStream.use { input ->
                FileOutputStream(destFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        onProgress(downloadedBytes, contentLength)
                    }
                }
            }

            if (contentLength > 0 && downloadedBytes < contentLength) {
                destFile.delete()
                throw IllegalStateException("Download incompleto: descarregados $downloadedBytes de $contentLength bytes.")
            }

            destFile
        } catch (e: Exception) {
            Log.e("UpdateUseCase", "Erro ao descarregar APK de atualização", e)
            null
        }
    }

    /**
     * Tenta abrir o instalador de pacotes nativo do Android via FileProvider.
     */
    fun abrirInstalador(apkFile: File): Boolean {
        return try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("UpdateUseCase", "Erro ao abrir o instalador do APK", e)
            false
        }
    }

    /**
     * Abre a página de Releases do repositório GitHub no browser do sistema.
     */
    fun abrirGitHubNoBrowser() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_RELEASES_URL)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("UpdateUseCase", "Erro ao abrir browser", e)
        }
    }

    /**
     * Limpa o prefixo 'v' ou sufixos de versão (ex: "-debug").
     */
    fun extrairVersaoSemantica(versionStr: String): String {
        return versionStr.trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore("-")
    }

    /**
     * Compara duas strings de versão semântica (ex: "1.10.0" vs "1.9.0").
     * Retorna > 0 se v1 > v2, < 0 se v1 < v2, e 0 se forem iguais.
     */
    fun compararVersoes(v1: String, v2: String): Int {
        val parts1 = v1.split(".").mapNotNull { it.toIntOrNull() }
        val parts2 = v2.split(".").mapNotNull { it.toIntOrNull() }

        val maxLength = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLength) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) {
                return p1.compareTo(p2)
            }
        }
        return 0
    }
}
