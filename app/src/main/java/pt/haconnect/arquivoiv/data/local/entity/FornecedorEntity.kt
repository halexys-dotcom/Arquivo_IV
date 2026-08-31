package pt.haconnect.arquivoiv.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 
 * Tabela de fornecedores conhecidos (Fase 9.1).
 * Usada para estatísticas e como base de conhecimento para o OCR (Fase 10).
 */
@Entity(
    tableName = "fornecedores",
    indices = [Index(value = ["nome"], unique = true)]
)
data class FornecedorEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "nome")
    val nome: String,

    @ColumnInfo(name = "data_primeiro_registo")
    val dataPrimeiroRegisto: Long = System.currentTimeMillis()
)

