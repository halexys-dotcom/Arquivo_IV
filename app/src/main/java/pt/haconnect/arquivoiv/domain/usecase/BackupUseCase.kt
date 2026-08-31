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
import pt.haconnect.arquivoiv.data.local.AppDatabase
import pt.haconnect.arquivoiv.data.repository.FaturaRepository
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class BackupInfo(
    val file: File,
    val name: String,
    val sizeFormatted: String,
    val sizeBytes: Long,
    val faturasCount: Int,
    val createdAtFormatted: String,
    val schemaVersion: Int
)

sealed interface BackupResult {
    data class Success(val message: String? = null) : BackupResult
    data class Error(val error: String) : BackupResult
}

@Singleton
class BackupUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDatabase: AppDatabase,
    private val faturaRepository: FaturaRepository
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 8
        const val BACKUP_DIR_NAME = "backups"
        const val MANIFEST_NAME = "manifest.json"
        const val DB_NAME = "arquivoiv_database"
        const val DOCUMENTS_DIR = "arquivoiv_documents"
    }

    private fun getBackupDir(): File =
        File(context.filesDir, BACKUP_DIR_NAME).apply { if (!exists()) mkdirs() }

    /**
     * Lista todos os backups locais em `context.filesDir/backups/`, ordenados do mais recente para o mais antigo.
     */
    suspend fun listarBackups(): List<BackupInfo> = withContext(Dispatchers.IO) {
        val dir = getBackupDir()
        val zipFiles = dir.listFiles { _, name -> name.lowercase().endsWith(".zip") } ?: emptyArray()

        zipFiles.mapNotNull { file ->
            lerInformacaoBackup(file)
        }.sortedByDescending { it.file.lastModified() }
    }

    /**
     * Calcula a soma do tamanho de todos os backups locais.
     */
    suspend fun calcularEspacoTotal(backups: List<BackupInfo>): String = withContext(Dispatchers.Default) {
        val totalBytes = backups.sumOf { it.sizeBytes }
        formatarTamanho(totalBytes)
    }

    /**
     * Cria um novo backup (.zip) com checkpoint do WAL, base de dados Room, pasta de anexos e manifest.json.
     */
    suspend fun criarBackup(): BackupResult = withContext(Dispatchers.IO) {
        try {
            // 1. Executa checkpoint da base de dados para consolidar o WAL no ficheiro principal
            try {
                appDatabase.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
            } catch (e: Exception) {
                Log.w("BackupUseCase", "Aviso no checkpoint do WAL: ${e.message}")
            }

            // 2. Ficheiros e pastas a incluir
            val dbFile = context.getDatabasePath(DB_NAME)
            val dbWal = context.getDatabasePath("$DB_NAME-wal")
            val dbShm = context.getDatabasePath("$DB_NAME-shm")
            val attachmentsDir = File(context.filesDir, DOCUMENTS_DIR)

            val faturasCount = faturaRepository.getAllList().size
            val now = Date()
            val dateFormatIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            val dateFormatDisplay = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val dateFormatFilename = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US)

            // 3. Gerar manifest.json
            val manifestJson = JSONObject().apply {
                put("appName", "Arquivo IV")
                put("appVersion", "1.0.0")
                put("schemaVersion", CURRENT_SCHEMA_VERSION)
                put("createdAt", dateFormatIso.format(now))
                put("createdAtFormatted", dateFormatDisplay.format(now))
                put("faturasCount", faturasCount)
            }.toString(2)

            // 4. Criar ficheiro ZIP de destino
            val zipFileName = "ArquivoIV_Backup_${dateFormatFilename.format(now)}.zip"
            val destZip = File(getBackupDir(), zipFileName)

            ZipOutputStream(BufferedOutputStream(FileOutputStream(destZip))).use { zos ->
                // Adicionar manifest.json
                adicionarEntry(zos, MANIFEST_NAME, manifestJson.toByteArray(Charsets.UTF_8))

                // Adicionar ficheiro DB principal
                if (dbFile.exists()) {
                    adicionarFicheiroEntry(zos, "db/$DB_NAME", dbFile)
                }
                if (dbWal.exists() && dbWal.length() > 0) {
                    adicionarFicheiroEntry(zos, "db/$DB_NAME-wal", dbWal)
                }
                if (dbShm.exists() && dbShm.length() > 0) {
                    adicionarFicheiroEntry(zos, "db/$DB_NAME-shm", dbShm)
                }

                // Adicionar anexos de arquivoiv_documents/
                if (attachmentsDir.exists() && attachmentsDir.isDirectory) {
                    attachmentsDir.listFiles()?.forEach { attachmentFile ->
                        if (attachmentFile.isFile && attachmentFile.length() > 0) {
                            adicionarFicheiroEntry(zos, "attachments/${attachmentFile.name}", attachmentFile)
                        }
                    }
                }
            }

            BackupResult.Success(destZip.name)
        } catch (e: Exception) {
            Log.e("BackupUseCase", "Erro ao criar backup", e)
            BackupResult.Error("Erro ao criar backup: ${e.message}")
        }
    }

    /**
     * Importa um ficheiro de backup externo (.zip escolhido pelo utilizador via SAF) para a pasta local.
     */
    suspend fun importarBackup(uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            val tmpFile = File(context.cacheDir, "tmp_import_${System.currentTimeMillis()}.zip")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tmpFile).use { output -> input.copyTo(output) }
            } ?: return@withContext BackupResult.Error("Não foi possível ler o ficheiro selecionado.")

            val info = lerInformacaoBackup(tmpFile)
            if (info == null) {
                tmpFile.delete()
                return@withContext BackupResult.Error("O ficheiro selecionado não é um backup válido do Arquivo IV.")
            }

            // Mover/copiar para a pasta de backups locais
            val destFile = File(getBackupDir(), info.name.ifBlank { "ArquivoIV_Imported_${System.currentTimeMillis()}.zip" })
            tmpFile.copyTo(destFile, overwrite = true)
            tmpFile.delete()

            BackupResult.Success(destFile.name)
        } catch (e: Exception) {
            Log.e("BackupUseCase", "Erro ao importar backup", e)
            BackupResult.Error("Erro ao importar backup: ${e.message}")
        }
    }

    /**
     * Restaura os dados de um backup ZIP (BD e anexos) substituindo os dados atuais.
     */
    suspend fun restaurarBackup(info: BackupInfo): BackupResult = withContext(Dispatchers.IO) {
        try {
            // 1. Validar versão do esquema no manifest
            if (info.schemaVersion != CURRENT_SCHEMA_VERSION) {
                return@withContext BackupResult.Error(
                    "O backup tem uma versão de esquema incompatível (versão ${info.schemaVersion} vs atual $CURRENT_SCHEMA_VERSION)."
                )
            }

            // 2. Fechar ligações à base de dados Room antes de substituir os ficheiros
            try {
                appDatabase.close()
            } catch (e: Exception) {
                Log.w("BackupUseCase", "Erro ao fechar BD para restauro: ${e.message}")
            }

            val dbFile = context.getDatabasePath(DB_NAME)
            val dbWal = context.getDatabasePath("$DB_NAME-wal")
            val dbShm = context.getDatabasePath("$DB_NAME-shm")
            val attachmentsDir = File(context.filesDir, DOCUMENTS_DIR).apply { if (!exists()) mkdirs() }

            // 3. Extrair conteúdo do ZIP
            ZipFile(info.file).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.isDirectory) continue

                    when {
                        entry.name == "db/$DB_NAME" -> {
                            zip.getInputStream(entry).use { input ->
                                FileOutputStream(dbFile).use { output -> input.copyTo(output) }
                            }
                        }
                        entry.name == "db/$DB_NAME-wal" -> {
                            zip.getInputStream(entry).use { input ->
                                FileOutputStream(dbWal).use { output -> input.copyTo(output) }
                            }
                        }
                        entry.name == "db/$DB_NAME-shm" -> {
                            zip.getInputStream(entry).use { input ->
                                FileOutputStream(dbShm).use { output -> input.copyTo(output) }
                            }
                        }
                        entry.name.startsWith("attachments/") -> {
                            val fileName = entry.name.removePrefix("attachments/")
                            if (fileName.isNotBlank()) {
                                val destAttachment = File(attachmentsDir, fileName)
                                zip.getInputStream(entry).use { input ->
                                    FileOutputStream(destAttachment).use { output -> input.copyTo(output) }
                                }
                            }
                        }
                    }
                }
            }

            BackupResult.Success()
        } catch (e: Exception) {
            Log.e("BackupUseCase", "Erro ao restaurar backup", e)
            BackupResult.Error("Erro ao restaurar backup: ${e.message}")
        }
    }

    /**
     * Elimina um ficheiro de backup individual.
     */
    suspend fun eliminarBackup(file: File): Boolean = withContext(Dispatchers.IO) {
        if (file.exists()) file.delete() else false
    }

    /**
     * Partilha um ficheiro de backup (.zip) via FileProvider e Intent.ACTION_SEND.
     */
    fun partilharBackup(file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Exportar Backup")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    private fun lerInformacaoBackup(file: File): BackupInfo? {
        return try {
            ZipFile(file).use { zip ->
                val manifestEntry = zip.getEntry(MANIFEST_NAME) ?: return null
                val jsonString = zip.getInputStream(manifestEntry).bufferedReader().use { it.readText() }
                val json = JSONObject(jsonString)

                val schemaVersion = json.optInt("schemaVersion", 0)
                val faturasCount = json.optInt("faturasCount", 0)
                val createdAtFormatted = json.optString("createdAtFormatted", "")

                BackupInfo(
                    file = file,
                    name = file.name,
                    sizeFormatted = formatarTamanho(file.length()),
                    sizeBytes = file.length(),
                    faturasCount = faturasCount,
                    createdAtFormatted = createdAtFormatted.ifBlank {
                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(file.lastModified()))
                    },
                    schemaVersion = schemaVersion
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun adicionarEntry(zos: ZipOutputStream, entryName: String, data: ByteArray) {
        val entry = ZipEntry(entryName)
        zos.putNextEntry(entry)
        zos.write(data)
        zos.closeEntry()
    }

    private fun adicionarFicheiroEntry(zos: ZipOutputStream, entryName: String, file: File) {
        val entry = ZipEntry(entryName)
        zos.putNextEntry(entry)
        FileInputStream(file).use { fis ->
            fis.copyTo(zos)
        }
        zos.closeEntry()
    }

    private fun formatarTamanho(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> String.format(Locale.getDefault(), "%.2f MB", mb)
            kb >= 1.0 -> String.format(Locale.getDefault(), "%.1f kB", kb)
            else -> "$bytes bytes"
        }
    }
}
