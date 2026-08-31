package pt.haconnect.arquivoiv.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import pt.haconnect.arquivoiv.data.local.entity.FaturaEntity
import kotlinx.coroutines.flow.Flow

/** CRUD básico — filtros de pesquisa por fornecedor/data chegam na Fase 4. */
@Dao
interface FaturaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(fatura: FaturaEntity): Long

    @Update
    suspend fun update(fatura: FaturaEntity)

    @Delete
    suspend fun delete(fatura: FaturaEntity)

    @Query("SELECT * FROM faturas WHERE id = :id")
    suspend fun getById(id: Long): FaturaEntity?

    @Query("SELECT * FROM faturas ORDER BY data_emissao DESC, id DESC")
    fun getAll(): Flow<List<FaturaEntity>>

    @Query("SELECT * FROM faturas ORDER BY data_emissao DESC, id DESC")
    suspend fun getAllList(): List<FaturaEntity>

    @Query("SELECT COUNT(*) FROM faturas")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(DISTINCT UPPER(fornecedor)) FROM faturas")
    suspend fun getCountFornecedores(): Int

    /** Fase 5 — anti-spam: marca o marco "a <30 dias de completar 5 anos" como notificado. */
    @Query("UPDATE faturas SET notificado_30dias = 1 WHERE id = :id")
    suspend fun marcarNotificado30dias(id: Long)

    /** Fase 5 — anti-spam: marca o marco "completou 5 anos" como notificado. */
    @Query("UPDATE faturas SET notificado_5anos = 1 WHERE id = :id")
    suspend fun marcarNotificado5anos(id: Long)

    /**
     * Fase 4: pesquisa combinada — fornecedor "contains" (LIKE, case-insensitive
     * para ASCII) e/ou intervalo inclusivo sobre data_emissao.
     * Sentinela 0 = critério não aplicado. Datas já normalizadas para
     * início (00:00) e fim (23:59:59.999) dos dias locais pelo chamador.
     */
    @Query(
        """
        SELECT * FROM faturas
        WHERE (:fornecedor = '' OR fornecedor LIKE '%' || :fornecedor || '%')
          AND (:dataInicio = 0 OR data_emissao >= :dataInicio)
          AND (:dataFim = 0 OR data_emissao <= :dataFim)
        ORDER BY data_emissao DESC, id DESC
        """
    )
    fun pesquisar(fornecedor: String, dataInicio: Long, dataFim: Long): Flow<List<FaturaEntity>>
}








