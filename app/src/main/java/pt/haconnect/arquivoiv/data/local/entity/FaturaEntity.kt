package pt.haconnect.arquivoiv.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Fatura arquivada — entidade única (o anexo é 1-para-1 e vive na própria fatura).
 * Schema fechado: fornecedor, número, data de emissão, anexo, data de arquivo.
 * Fase 5: + flags de controlo de notificação de retenção (anti-spam, 1 notificação por marco).
 */
@Entity(tableName = "faturas")
data class FaturaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "fornecedor")
    val fornecedor: String,

    @ColumnInfo(name = "numero_fatura")
    val numeroFatura: String,

    /** Timestamp (epoch millis) da emissão da fatura pelo fornecedor. */
    @ColumnInfo(name = "data_emissao")
    val dataEmissao: Long,

    /** Caminho absoluto do anexo único (PDF ou foto). */
    @ColumnInfo(name = "caminho_anexo")
    val caminhoAnexo: String,

    /** Timestamp de arquivo na app — base do cálculo dos 5 anos de retenção (Fase 5). */
    @ColumnInfo(name = "data_insercao")
    val dataInsercao: Long = System.currentTimeMillis(),

    /** True quando a fatura já foi notificada a <30 dias de completar 5 anos. */
    @ColumnInfo(name = "notificado_30dias", defaultValue = "0")
    val notificado30dias: Boolean = false,

    /** True quando a fatura já foi notificada após completar 5 anos. */
    @ColumnInfo(name = "notificado_5anos", defaultValue = "0")
    val notificado5anos: Boolean = false
)








