package pt.haconnect.arquivoiv.data.repository

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import pt.haconnect.arquivoiv.data.local.dao.FaturaDao
import pt.haconnect.arquivoiv.data.local.dao.FornecedorDao
import pt.haconnect.arquivoiv.data.local.entity.FaturaEntity
import pt.haconnect.arquivoiv.data.local.entity.FornecedorEntity
import pt.haconnect.arquivoiv.domain.model.Fatura
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FaturaRepository @Inject constructor(
    private val faturaDao: FaturaDao,
    private val fornecedorDao: FornecedorDao,
    @ApplicationContext private val context: Context
) {

    fun getAll(): Flow<List<Fatura>> = faturaDao.getAll().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun getAllList(): List<Fatura> = faturaDao.getAllList().map { it.toDomain() }

    suspend fun getById(id: Long): Fatura? = faturaDao.getById(id)?.toDomain()

    /** Fornecedores conhecidos (Fase 9.1). */
    fun getFornecedores(): Flow<List<String>> = fornecedorDao.getAll().map { list ->
        list.map { it.nome }
    }

    /** Fase 4: pesquisa combinada (fornecedor contém + intervalo de datas opcionais). */
    fun pesquisar(fornecedor: String, dataInicio: Long?, dataFim: Long?): Flow<List<Fatura>> =
        faturaDao.pesquisar(
            fornecedor = fornecedor.trim(),
            dataInicio = dataInicio ?: 0L,
            dataFim = dataFim ?: 0L
        ).map { entities -> entities.map { it.toDomain() } }

    suspend fun getCount(): Int = faturaDao.getCount()

    suspend fun getCountFornecedores(): Int = faturaDao.getCountFornecedores()

    /** Fase 5 — marca o marco "a <30 dias dos 5 anos" como já notificado. */
    suspend fun marcarNotificado30dias(id: Long) = faturaDao.marcarNotificado30dias(id)

    /** Fase 5 — marca o marco "completou 5 anos" como já notificado. */
    suspend fun marcarNotificado5anos(id: Long) = faturaDao.marcarNotificado5anos(id)

    /** Faturas arquivadas nos últimos N dias (base futura da retenção de 5 anos). */
    suspend fun getInseridasNosUltimosDias(dias: Int): Int {
        val limite = System.currentTimeMillis() - dias * 24L * 60 * 60 * 1000
        return faturaDao.getAllList().count { it.dataInsercao >= limite }
    }

    suspend fun getUltimasFaturas(limit: Int = 5): List<Fatura> =
        faturaDao.getAllList()
            .sortedByDescending { it.dataInsercao }
            .take(limit)
            .map { it.toDomain() }

    suspend fun insert(fatura: Fatura): Long {
        assegurarFornecedorExiste(fatura.fornecedor)
        return faturaDao.insert(fatura.toEntity())
    }

    suspend fun update(fatura: Fatura) {
        assegurarFornecedorExiste(fatura.fornecedor)
        faturaDao.update(fatura.toEntity())
    }

    private suspend fun assegurarFornecedorExiste(nome: String) {
        val limpo = nome.trim()
        if (limpo.isBlank()) return
        val existe = fornecedorDao.getByNome(limpo)
        if (existe == null) {
            fornecedorDao.insert(FornecedorEntity(nome = limpo))
        }
    }

    suspend fun delete(fatura: Fatura) {
        apagarAnexoSeInterno(fatura.caminhoAnexo)
        faturaDao.delete(fatura.toEntity())
    }

    /**
     * Copia o anexo único (PDF/foto) escolhido via SAF para o armazenamento
     * interno da app e devolve o caminho absoluto a guardar em caminhoAnexo.
     */
    fun copiarAnexo(uri: Uri): String {
        val nomeOriginal = obterNomeFicheiro(uri) ?: "anexo_${System.currentTimeMillis()}"
        val dir = File(context.filesDir, PASTA_ANEXOS).apply { if (!exists()) mkdirs() }
        val destino = File(dir, "${System.currentTimeMillis()}_$nomeOriginal")

        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destino).use { output -> input.copyTo(output) }
        } ?: throw IllegalStateException("Não foi possível ler o ficheiro selecionado")

        return destino.absolutePath
    }

    /**
     * Remove um anexo órfão (quando o utilizador substitui o anexo numa edição).
     * Só remove ficheiros geridos pela app (dentro da pasta de anexos).
     */
    fun apagarAnexoSeInterno(caminho: String) {
        if (caminho.contains(PASTA_ANEXOS)) {
            File(caminho).takeIf { it.exists() }?.delete()
        }
    }

    private fun obterNomeFicheiro(uri: Uri): String? {
        var nome: String? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) nome = cursor.getString(idx)
            }
        }
        return nome
    }

    private fun FaturaEntity.toDomain(): Fatura = Fatura(
        id = id,
        fornecedor = fornecedor,
        numeroFatura = numeroFatura,
        dataEmissao = dataEmissao,
        caminhoAnexo = caminhoAnexo,
        dataInsercao = dataInsercao,
        notificado30dias = notificado30dias,
        notificado5anos = notificado5anos
    )

    private fun Fatura.toEntity(): FaturaEntity = FaturaEntity(
        id = id,
        fornecedor = fornecedor,
        numeroFatura = numeroFatura,
        dataEmissao = dataEmissao,
        caminhoAnexo = caminhoAnexo,
        dataInsercao = dataInsercao,
        notificado30dias = notificado30dias,
        notificado5anos = notificado5anos
    )

    companion object {
        const val PASTA_ANEXOS = "arquivoiv_documents"
    }
}








