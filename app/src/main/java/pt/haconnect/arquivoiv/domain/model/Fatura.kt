package pt.haconnect.arquivoiv.domain.model

/** Modelo de domínio da fatura arquivada (espelho de FaturaEntity). */
data class Fatura(
    val id: Long = 0,
    val fornecedor: String,
    val numeroFatura: String,
    val dataEmissao: Long,
    val caminhoAnexo: String,
    val dataInsercao: Long = System.currentTimeMillis(),
    val notificado30dias: Boolean = false,
    val notificado5anos: Boolean = false
)








