package pt.haconnect.arquivoiv.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

data class OcrResult(
    val fornecedor: String? = null,
    val numeroFatura: String? = null,
    val dataEmissao: Long? = null
)

@Singleton
class OcrExtrairDadosUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun extrair(
        uri: Uri,
        nomesConhecidos: List<String>,
        nomeFicheiro: String? = null,
        isFromSaf: Boolean = false
    ): OcrResult {
        return try {
            val type = context.contentResolver.getType(uri)
            val visionText = if (type == "application/pdf") {
                val bitmap = renderPdfFirstPage(uri)
                if (bitmap != null) {
                    recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
                } else {
                    null
                }
            } else {
                recognizer.process(InputImage.fromFilePath(context, uri)).await()
            }

            if (visionText != null) {
                processarTexto(visionText, nomesConhecidos, nomeFicheiro, isFromSaf)
            } else {
                val numFallback = if (isFromSaf) nomeFicheiro?.let { extrairNumeroDoNomeFicheiro(it) } else null
                OcrResult(numeroFatura = numFallback)
            }
        } catch (e: Exception) {
            Log.e("OCR_ERROR", "Erro no processamento OCR", e)
            val numFallback = if (isFromSaf) nomeFicheiro?.let { extrairNumeroDoNomeFicheiro(it) } else null
            OcrResult(numeroFatura = numFallback)
        }
    }

    private fun renderPdfFirstPage(uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val renderer = PdfRenderer(pfd)
                if (renderer.pageCount == 0) {
                    renderer.close()
                    return null
                }
                val page = renderer.openPage(0)
                val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                renderer.close()
                bitmap
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun processarTexto(
        text: Text,
        nomesConhecidos: List<String>,
        nomeFicheiro: String?,
        isFromSaf: Boolean
    ): OcrResult {
        Log.d("OCR_RAW", "Texto Bruto Extraído: \n${text.text}")
        
        val lines = text.textBlocks.flatMap { it.lines }
        if (lines.isEmpty()) {
            val numFallback = if (isFromSaf) nomeFicheiro?.let { extrairNumeroDoNomeFicheiro(it) } else null
            return OcrResult(numeroFatura = numFallback)
        }

        val allBottoms = lines.mapNotNull { it.boundingBox?.bottom }
        val maxHeight = if (allBottoms.isNotEmpty()) allBottoms.max() else 1000
        val topThird = maxHeight / 3

        // 1. Extrair Fornecedor
        var fornecedor = encontrarFornecedorConhecido(lines, nomesConhecidos)
        if (fornecedor == null) {
            fornecedor = heuristicaFornecedorNovo(lines, topThird)
        }

        // 2. Extrair Data
        val data = extrairData(lines)

        // 3. Extrair Número Fatura
        val numero = extrairNumero(lines, nomeFicheiro, isFromSaf)

        Log.d("OCR_RESULT", "Sugeridos -> Fornecedor: $fornecedor, Numero: $numero, Data: $data")
        return OcrResult(fornecedor, numero, data)
    }

    private fun encontrarFornecedorConhecido(
        lines: List<Text.Line>,
        nomesConhecidos: List<String>
    ): String? {
        for (nome in nomesConhecidos) {
            val limpo = nome.trim()
            if (limpo.length < 3) continue
            
            val pattern = Pattern.compile(Pattern.quote(limpo), Pattern.CASE_INSENSITIVE)
            if (lines.any { pattern.matcher(it.text).find() }) {
                return nome
            }
        }
        
        for (nome in nomesConhecidos) {
            val limpo = nome.trim().uppercase()
            if (limpo.length <= 6) continue
            
            for (line in lines) {
                val lineText = line.text.uppercase()
                val words = lineText.split("\\s+".toRegex())
                for (word in words) {
                    if (word.length >= 5 && distanciaLevenshtein(limpo, word) <= 2) {
                        return nome
                    }
                }
                if (distanciaLevenshtein(limpo, lineText) <= 2 || lineText.contains(limpo)) {
                    return nome
                }
            }
        }
        return null
    }

    private fun heuristicaFornecedorNovo(lines: List<Text.Line>, topLimit: Int): String? {
        val topLines = lines.filter { (it.boundingBox?.top ?: 0) < topLimit }
        
        val ignoreWords = listOf(
            "FATURA", "FACTURA", "RECIBO", "PÁGINA", "PAGINA", "ORIGINAL", "DUPLICADO",
            "DOCUMENTO", "NIF", "CONTRIBUINTE", "NIPC", "TEL", "TELEFONE",
            "EMAIL", "HTTP", "HTTPS", "WWW", "VIA", "COPIA", "CÓPIA"
        )

        val candidates = topLines.filter { line ->
            val upper = line.text.uppercase()
            ignoreWords.none { upper.contains(it) } && line.text.trim().length > 2
        }

        val tallestLine = candidates.maxByOrNull { it.boundingBox?.height() ?: 0 }
        
        val nifPattern = Pattern.compile("(?i)(NIF|NIPC|CONTRIBUINTE|CONTRIB)[:.\\s]*([0-9]{9})")
        val nifLineIndex = lines.indexOfFirst { nifPattern.matcher(it.text).find() }
        val beforeNif = if (nifLineIndex > 0) lines[nifLineIndex - 1].text else null

        if (tallestLine != null && (tallestLine.boundingBox?.height() ?: 0) > 25) {
            val text = tallestLine.text.trim()
            if (text.length > 2 && ignoreWords.none { text.contains(it, ignoreCase = true) }) {
                return text
            }
        }

        if (beforeNif != null) {
            val text = beforeNif.trim()
            if (text.length > 3 && ignoreWords.none { text.contains(it, ignoreCase = true) }) {
                return text
            }
        }

        return null
    }

    private fun extrairData(lines: List<Text.Line>): Long? {
        val today = LocalDate.now()
        val candidates = mutableListOf<Pair<LocalDate, Boolean>>()

        val extensoRegex = Regex("(\\d{1,2})\\s+de\\s+(\\w+)\\s+de\\s+(\\d{4})", RegexOption.IGNORE_CASE)
        val isoRegex = Regex("\\b(\\d{4})[/.-](\\d{1,2})[/.-](\\d{1,2})\\b")
        val numericRegex = Regex("\\b(\\d{1,2})\\s*[/.-]\\s*(\\d{1,2})\\s*[/.-]\\s*(\\d{2,4})\\b")

        for (line in lines) {
            val lineText = line.text.trim()
            val isPriorityLine = lineText.contains("EMISS", ignoreCase = true) ||
                                 lineText.contains("DATA", ignoreCase = true) ||
                                 lineText.contains("DATE", ignoreCase = true)

            extensoRegex.findAll(lineText).forEach { match ->
                tentarParseExtenso(match.value)?.let { date ->
                    if (!date.isAfter(today)) candidates.add(Pair(date, isPriorityLine))
                }
            }

            isoRegex.findAll(lineText).forEach { match ->
                tentarParseIso(match.value)?.let { date ->
                    if (!date.isAfter(today)) candidates.add(Pair(date, isPriorityLine))
                }
            }

            numericRegex.findAll(lineText).forEach { match ->
                tentarParseNumerico(match.value)?.let { date ->
                    if (!date.isAfter(today)) candidates.add(Pair(date, isPriorityLine))
                }
            }
        }

        val priorityDate = candidates.firstOrNull { it.second }?.first
        if (priorityDate != null) {
            return dateToMillis(priorityDate)
        }

        return candidates.map { it.first }.minOrNull()?.let { dateToMillis(it) }
    }

    private fun dateToMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun tentarParseExtenso(str: String): LocalDate? {
        return try {
            val extensoPattern = Pattern.compile("(\\d{1,2})\\s*de\\s*(\\w+)\\s*de\\s*(\\d{4})", Pattern.CASE_INSENSITIVE)
            val matcher = extensoPattern.matcher(str)
            if (matcher.find()) {
                val dia = matcher.group(1)?.toInt() ?: return null
                val mesStr = matcher.group(2)?.lowercase() ?: return null
                val ano = matcher.group(3)?.toInt() ?: return null
                
                val meses = listOf("janeiro", "fevereiro", "março", "marco", "abril", "maio", "junho", "julho", "agosto", "setembro", "outubro", "novembro", "dezembro")
                val mes = meses.indexOf(mesStr) + 1
                if (mes > 0) LocalDate.of(ano, mes, dia) else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun tentarParseIso(str: String): LocalDate? {
        return try {
            val clean = str.replace(".", "-").replace("/", "-").trim()
            LocalDate.parse(clean, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        } catch (e: Exception) {
            null
        }
    }

    private fun tentarParseNumerico(str: String): LocalDate? {
        val clean = str.replace(" ", "").replace(".", "/").replace("-", "/")
        val parts = clean.split("/")
        if (parts.size != 3) return null

        val p1 = parts[0].toIntOrNull() ?: return null
        val p2 = parts[1].toIntOrNull() ?: return null
        var p3 = parts[2].toIntOrNull() ?: return null

        if (p3 < 100) {
            p3 += 2000
        }

        return try {
            LocalDate.of(p3, p2, p1)
        } catch (e: Exception) {
            null
        }
    }

    private fun extrairNumero(
        lines: List<Text.Line>,
        nomeFicheiro: String?,
        isFromSaf: Boolean
    ): String? {
        val faturaLabels = listOf(
            "FATURA-RECIBO", "FACTURA-RECIBO",
            "FATURA REPOSITORIO", "FATURA REPOSITÓRIO",
            "FACTURA REPOSITORIO", "FACTURA REPOSITÓRIO",
            "FATURA", "FACTURA", "FT", "FA", "FS",
            "DOC N.º", "DOC Nº", "Nº DOCUMENTO", "N.º DOCUMENTO", "Nº DOC", "N.º DOC",
            "V/ REF", "V/REF"
        )

        val reciboLabels = listOf(
            "RECIBO", "FR", "REC", "RC", "NC", "ND", "REF.ª", "REFª"
        )

        // 1. First Pass: Search for Fatura/Factura high priority matches
        val faturaMatch = procurarCandidatoNumero(lines, faturaLabels)
        if (faturaMatch != null) {
            return faturaMatch
        }

        // 2. Second Pass: Search for Recibo matches
        val reciboMatch = procurarCandidatoNumero(lines, reciboLabels)
        if (reciboMatch != null) {
            return reciboMatch
        }

        // 3. Third Pass: Standalone reference pattern search across valid lines
        val standalonePattern = Pattern.compile(
            "\\b([A-Z]{1,4}\\s*\\d{1,8}/\\d{1,8}|\\d{4}/\\d{1,8}|[A-Z]{1,4}\\d{4,12})\\b",
            Pattern.CASE_INSENSITIVE
        )

        for (line in lines) {
            val lineText = line.text.trim()
            if (ehLinhaInvalida(lineText)) continue

            val matcher = standalonePattern.matcher(lineText)
            while (matcher.find()) {
                val candidate = matcher.group(1)?.trim()
                if (validarCandidatoNumero(candidate)) {
                    return candidate
                }
            }
        }

        // 4. Fallback: Filename check ONLY if file chosen via SAF
        if (isFromSaf && !nomeFicheiro.isNullOrBlank()) {
            val candidateFromFilename = extrairNumeroDoNomeFicheiro(nomeFicheiro)
            if (candidateFromFilename != null) {
                return candidateFromFilename
            }
        }

        return null
    }

    private fun procurarCandidatoNumero(
        lines: List<Text.Line>,
        labels: List<String>
    ): String? {
        val labelPattern = Pattern.compile(
            "(?:${labels.joinToString("|")})[:.\\s|/\\-]*([A-Z0-9/\\-_]{2,25})",
            Pattern.CASE_INSENSITIVE
        )

        for (i in lines.indices) {
            val lineText = lines[i].text.trim()
            if (ehLinhaInvalida(lineText)) continue

            val matcher = labelPattern.matcher(lineText)
            if (matcher.find()) {
                val candidate = matcher.group(1)?.trim()
                if (validarCandidatoNumero(candidate)) {
                    return candidate
                }
            }

            val isLabelOnly = labels.any { label ->
                lineText.equals(label, ignoreCase = true) ||
                lineText.equals("$label:", ignoreCase = true) ||
                lineText.equals("$label N.º", ignoreCase = true) ||
                lineText.equals("DOC. N.º", ignoreCase = true) ||
                lineText.equals("DOCUMENTO", ignoreCase = true)
            }

            if (isLabelOnly && i + 1 < lines.size) {
                val nextLineText = lines[i + 1].text.trim()
                if (!ehLinhaInvalida(nextLineText)) {
                    val candidate = nextLineText.split(" ", ":", "|").firstOrNull()?.trim()
                    if (validarCandidatoNumero(candidate)) {
                        return candidate
                    }
                }
            }
        }

        return null
    }

    private fun ehLinhaInvalida(lineText: String): Boolean {
        val upper = lineText.uppercase()

        if (upper.contains("ATCUD")) return true
        if (upper.contains("NIF") || upper.contains("NIPC") || upper.contains("CONTRIBUINTE") || upper.contains("CONTRIB") || upper.contains("NºCONT")) return true
        if (upper.contains("BENEFICIÁRIO") || upper.contains("BENEFICIARIO")) return true
        if (upper.contains("/AT")) return true

        val addressKeywords = listOf(
            "RUA", "AVENIDA", "AV.", "ESTRADA", "LARGO", "PRAÇA", "PRACA",
            "TRAVESSA", "ALAMEDA", "PRACETA", "URBANIZAÇÃO", "URBANIZACAO",
            "EDIFÍCIO", "EDIFICIO", "BAIRRO", "CAMINHO", "ZONA", "PARQUE",
            "LISBOA", "PORTO", "COIMBRA", "BRAGA", "SETÚBAL", "SETUBAL",
            "MOSCAVIDE", "SINTRA", "CASCAIS", "AMADORA", "ALMADA"
        )
        if (addressKeywords.any { upper.contains(it) }) return true

        if (Regex("\\b\\d{4}-\\d{3}\\b").containsMatchIn(lineText)) return true

        return false
    }

    private fun validarCandidatoNumero(candidate: String?): Boolean {
        if (candidate == null) return false
        val clean = candidate.trim()
        if (clean.length < 3 || clean.length > 25) return false
        if (!clean.any { it.isDigit() }) return false

        if (clean.matches(Regex("^\\d{4}-\\d{3}$")) || clean.matches(Regex("^\\d{4}$"))) return false
        if (clean.matches(Regex("^[12356789]\\d{8}$"))) return false
        if (clean.matches(Regex("^\\d{2,4}[/.-]\\d{2}[/.-]\\d{2,4}$"))) return false

        return true
    }

    private fun extrairNumeroDoNomeFicheiro(nomeFicheiro: String): String? {
        val baseName = nomeFicheiro.substringBeforeLast('.')
            .replace(Regex("[%_\\-]+"), " ")
            .trim()

        val lower = baseName.lowercase()
        val genericos = listOf(
            "documento", "fatura", "factura", "recibo", "invoice", "scan",
            "imagem", "image", "img", "photo", "foto", "download", "file",
            "ficheiro", "arquivo", "cam", "screenshot", "novo", "new"
        )

        val words = lower.split("\\s+".toRegex())
        if (words.any { it in genericos } && !baseName.any { it.isDigit() }) {
            return null
        }

        val filteredWords = words.filterNot { it in genericos }
        val cleanName = filteredWords.joinToString("").trim()

        if (cleanName.length >= 4 && cleanName.any { it.isDigit() }) {
            val candidate = baseName.replace(" ", "")
            if (candidate.length >= 4 && candidate.length <= 25 && !candidate.matches(Regex("^\\d{9}$"))) {
                return candidate
            }
        }

        return null
    }

    private fun distanciaLevenshtein(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
            }
        }
        return dp[s1.length][s2.length]
    }
}
