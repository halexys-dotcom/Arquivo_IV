package pt.haconnect.arquivoiv.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import pt.haconnect.arquivoiv.data.local.entity.FornecedorEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FornecedorDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(fornecedor: FornecedorEntity): Long

    @Query("SELECT * FROM fornecedores ORDER BY nome ASC")
    fun getAll(): Flow<List<FornecedorEntity>>

    @Query("SELECT nome FROM fornecedores ORDER BY nome ASC")
    suspend fun getTodosNomes(): List<String>

    @Query("SELECT COUNT(*) FROM fornecedores")
    suspend fun getCount(): Int

    @Query("SELECT * FROM fornecedores WHERE UPPER(TRIM(nome)) = UPPER(TRIM(:nome)) LIMIT 1")
    suspend fun getByNome(nome: String): FornecedorEntity?
}

