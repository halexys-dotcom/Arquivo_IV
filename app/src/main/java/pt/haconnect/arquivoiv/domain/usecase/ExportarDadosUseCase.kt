package pt.haconnect.arquivoiv.domain.usecase

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import pt.haconnect.arquivoiv.R
import pt.haconnect.arquivoiv.data.repository.FaturaRepository
import pt.haconnect.arquivoiv.domain.model.Fatura
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileWriter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.layout.element.Image
import java.io.ByteArrayOutputStream
import java.time.LocalDate

/** Exportação PDF/CSV das faturas arquivadas (iText 7). */
@Singleton
class ExportarDadosUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val faturaRepository: FaturaRepository
) {
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        .withZone(ZoneId.systemDefault())

    private fun formatarData(timestamp: Long): String =
        dateFormatter.format(Instant.ofEpochMilli(timestamp))

    /**
     * Exporta uma única fatura com o seu anexo visual.
     * Gera um PDF com cabeçalho de dados e a imagem do anexo redimensionada.
     */
    suspend fun exportarFaturaIndividual(fatura: Fatura): File? {
        val dir = context.externalCacheDir ?: context.cacheDir
        val nomeLimpo = "${fatura.fornecedor}_${fatura.numeroFatura}"
            .replace(Regex("[^a-zA-Z0-9]"), "_")
        val file = File(dir, "ArquivoIV_$nomeLimpo.pdf")

        return try {
            PdfWriter(file).use { writer ->
                val pdf = PdfDocument(writer)
                val document = Document(pdf)

                // Cabeçalho
                document.add(Paragraph(context.getString(R.string.invoice_detail_title))
                    .setBold().setFontSize(16f))
                document.add(Paragraph("${context.getString(R.string.invoice_field_fornecedor)}: ${fatura.fornecedor}"))
                document.add(Paragraph("${context.getString(R.string.invoice_field_numero)}: ${fatura.numeroFatura}"))
                document.add(Paragraph("${context.getString(R.string.invoice_field_data_emissao)}: ${formatarData(fatura.dataEmissao)}"))
                document.add(Paragraph("\n"))

                // Anexo
                val anexoFile = File(fatura.caminhoAnexo)
                if (anexoFile.exists()) {
                    val bitmap = if (fatura.caminhoAnexo.lowercase().endsWith(".pdf")) {
                        renderPdfFirstPage(anexoFile)
                    } else {
                        prepararImagem(anexoFile)
                    }

                    if (bitmap != null) {
                        val stream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                        val imageData = ImageDataFactory.create(stream.toByteArray())
                        val image = Image(imageData).setAutoScale(true)
                        document.add(image)
                    } else {
                        document.add(Paragraph("Anexo indisponível (erro na leitura)"))
                    }
                } else {
                    document.add(Paragraph("Anexo indisponível (ficheiro não encontrado)"))
                }

                document.close()
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun renderPdfFirstPage(file: File): Bitmap? {
        return try {
            val pfd = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            if (renderer.pageCount == 0) return null
            val page = renderer.openPage(0)
            val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            pfd.close()
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun prepararImagem(file: File): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, options)
            
            val maxDim = 1500
            var sampleSize = 1
            if (options.outHeight > maxDim || options.outWidth > maxDim) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while (halfHeight / sampleSize >= maxDim || halfWidth / sampleSize >= maxDim) {
                    sampleSize *= 2
                }
            }
            
            BitmapFactory.Options().run {
                inSampleSize = sampleSize
                BitmapFactory.decodeFile(file.absolutePath, this)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Exporta todas as faturas.
     * Para exportar um subconjunto filtrado, usar [exportarPdf] com a lista desejada.
     */
    suspend fun exportarPdf(): File? = exportarPdf(faturaRepository.getAllList())

    /**
     * Exporta um subconjunto de faturas (ex.: resultado da pesquisa da Fase 4).
     * Schema: fornecedor, número fatura, data emissão, data inserção. Sem categoria.
     */
    suspend fun exportarPdf(faturas: List<Fatura>): File? {
        if (faturas.isEmpty()) return null

        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        val file = File(dir, "ArquivoIV_Export_${System.currentTimeMillis()}.pdf")

        PdfWriter(file).use { writer ->
            val pdf = PdfDocument(writer)
            val document = Document(pdf)

            document.add(Paragraph(context.getString(R.string.pdf_title))
                .setBold().setFontSize(18f).setTextAlignment(TextAlignment.CENTER))
            document.add(Paragraph(context.getString(R.string.pdf_generated_on, formatarData(System.currentTimeMillis())))
                .setTextAlignment(TextAlignment.CENTER))
            document.add(Paragraph("\n"))

            val table = Table(3)
            table.addHeaderCell(Cell().add(Paragraph(context.getString(R.string.pdf_header_fornecedor)).setBold()))
            table.addHeaderCell(Cell().add(Paragraph(context.getString(R.string.pdf_header_numero)).setBold()))
            table.addHeaderCell(Cell().add(Paragraph(context.getString(R.string.pdf_header_data_emissao)).setBold()))

            faturas.forEach { f ->
                table.addCell(f.fornecedor)
                table.addCell(f.numeroFatura)
                table.addCell(formatarData(f.dataEmissao))
            }
            document.add(table)
            document.close()
        }
        return file
    }

    suspend fun exportarCsv(): File? = exportarCsv(faturaRepository.getAllList())

    /**
     * Exporta um subconjunto de faturas (ex.: resultado da pesquisa da Fase 4).
     * Usa ponto-e-vírgula como separador (convenção PT) para evitar ambiguidade
     * com o ponto decimal dos valores.
     */
    suspend fun exportarCsv(faturas: List<Fatura>): File? {
        if (faturas.isEmpty()) return null

        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        val sep = ";"
        val file = File(dir, "ArquivoIV_Export_${System.currentTimeMillis()}.csv")

        FileWriter(file).use { writer ->
            writer.append("${context.getString(R.string.csv_header_fornecedor)}${sep}${context.getString(R.string.csv_header_numero)}${sep}${context.getString(R.string.csv_header_data_emissao)}${sep}${context.getString(R.string.csv_header_data_insercao)}\n")
            faturas.forEach { f ->
                writer.append("${escapeCsv(f.fornecedor, sep)}${sep}")
                writer.append("${escapeCsv(f.numeroFatura, sep)}${sep}")
                writer.append("${formatarData(f.dataEmissao)}${sep}")
                writer.append("${formatarData(f.dataInsercao)}\n")
            }
        }
        return file
    }

    fun shareFile(file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (file.extension == "csv") "text/csv" else "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, context.getString(R.string.pdf_share_title))
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    private fun escapeCsv(value: String, separator: String = ","): String {
        val sep = if (separator == ";") ";" else ","
        return if (value.contains(sep) || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else value
    }
}








