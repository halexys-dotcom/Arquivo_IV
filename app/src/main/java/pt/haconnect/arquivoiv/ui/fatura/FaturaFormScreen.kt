package pt.haconnect.arquivoiv.ui.fatura

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import pt.haconnect.arquivoiv.R
import pt.haconnect.arquivoiv.data.repository.FaturaRepository
import pt.haconnect.arquivoiv.domain.model.Fatura
import pt.haconnect.arquivoiv.ui.components.PremiumHeader
import pt.haconnect.arquivoiv.ui.theme.Error
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private const val IMG_EXTS = "jpg,jpeg,png,webp,heic"

/** Estado do anexo único escolhido no formulário. */
private sealed interface AnexoUi {
    data class FotoTmp(val ficheiro: File) : AnexoUi
    data class Saf(val uri: Uri, val nome: String) : AnexoUi
    data class Existente(val caminho: String) : AnexoUi
}

private fun AnexoUi.eImagem(): Boolean = when (this) {
    is AnexoUi.FotoTmp -> true
    is AnexoUi.Saf -> nome.substringAfterLast('.', "").lowercase() in IMG_EXTS.split(',')
    is AnexoUi.Existente -> caminho.substringAfterLast('.', "").lowercase() in IMG_EXTS.split(',')
}

private fun AnexoUi.nomeFicheiro(): String = when (this) {
    is AnexoUi.FotoTmp -> ficheiro.name
    is AnexoUi.Saf -> nome
    is AnexoUi.Existente -> File(caminho).name
}

private val displayDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaturaFormScreen(
    faturaId: Long?,
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: FaturaViewModel = hiltViewModel()
) {
    val isEditing = faturaId != null
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var fornecedor by rememberSaveable { mutableStateOf("") }
    var numero by rememberSaveable { mutableStateOf("") }
    var dataEpochDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var anexoKind by rememberSaveable { mutableStateOf<String?>(null) }
    var anexoPathOrUri by rememberSaveable { mutableStateOf<String?>(null) }
    var anexoName by rememberSaveable { mutableStateOf<String?>(null) }
    var anexoOriginalPath by rememberSaveable { mutableStateOf<String?>(null) }
    var mostrarPicker by rememberSaveable { mutableStateOf(false) }

    var preenchidoOcrFornecedor by rememberSaveable { mutableStateOf(false) }
    var preenchidoOcrNumero by rememberSaveable { mutableStateOf(false) }
    var preenchidoOcrData by rememberSaveable { mutableStateOf(false) }

    var erroFornecedor by rememberSaveable { mutableStateOf<Int?>(null) }
    var erroNumero by rememberSaveable { mutableStateOf<Int?>(null) }
    var erroData by rememberSaveable { mutableStateOf<Int?>(null) }
    var erroAnexo by rememberSaveable { mutableStateOf<Int?>(null) }
    var erroGeral by rememberSaveable { mutableStateOf<String?>(null) }
    var isSaving by rememberSaveable { mutableStateOf(false) }
    var carregado by rememberSaveable { mutableStateOf(false) }

    val dataSelecionada: LocalDate? = dataEpochDay?.let { LocalDate.ofEpochDay(it) }

    fun setDataSelecionada(date: LocalDate?) {
        dataEpochDay = date?.toEpochDay()
    }

    val anexo: AnexoUi? = when (anexoKind) {
        "tmp" -> anexoPathOrUri?.let { AnexoUi.FotoTmp(File(it)) }
        "saf" -> if (anexoPathOrUri != null && anexoName != null) AnexoUi.Saf(Uri.parse(anexoPathOrUri), anexoName!!) else null
        "existente" -> anexoPathOrUri?.let { AnexoUi.Existente(it) }
        else -> null
    }

    fun setAnexoUi(a: AnexoUi?) {
        when (a) {
            is AnexoUi.FotoTmp -> {
                anexoKind = "tmp"
                anexoPathOrUri = a.ficheiro.absolutePath
                anexoName = a.ficheiro.name
            }
            is AnexoUi.Saf -> {
                anexoKind = "saf"
                anexoPathOrUri = a.uri.toString()
                anexoName = a.nome
            }
            is AnexoUi.Existente -> {
                anexoKind = "existente"
                anexoPathOrUri = a.caminho
                anexoName = File(a.caminho).name
            }
            null -> {
                anexoKind = null
                anexoPathOrUri = null
                anexoName = null
            }
        }
    }

    val detail by viewModel.detailState.collectAsStateWithLifecycle()

    LaunchedEffect(faturaId) { faturaId?.let { viewModel.loadFatura(it) } }

    LaunchedEffect(detail.fatura) {
        detail.fatura?.let { f ->
            if (!carregado) {
                fornecedor = f.fornecedor
                numero = f.numeroFatura
                setDataSelecionada(
                    Instant.ofEpochMilli(f.dataEmissao).atZone(ZoneId.systemDefault()).toLocalDate()
                )
                anexoOriginalPath = f.caminhoAnexo
                setAnexoUi(AnexoUi.Existente(f.caminhoAnexo))
                carregado = true
            }
        }
    }

    // --- OCR Handling ---
    LaunchedEffect(detail.ocrResult) {
        detail.ocrResult?.let { res ->
            res.fornecedor?.let { 
                if (fornecedor.isBlank()) {
                    fornecedor = it
                    preenchidoOcrFornecedor = true
                }
            }
            res.numeroFatura?.let {
                if (numero.isBlank()) {
                    numero = it
                    preenchidoOcrNumero = true
                }
            }
            res.dataEmissao?.let {
                if (dataSelecionada == null) {
                    setDataSelecionada(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate())
                    preenchidoOcrData = true
                }
            }
            viewModel.clearOcrResult()
        }
    }

    // --- Câmara: foto tirada na hora ---
    var cameraFilePath by rememberSaveable { mutableStateOf<String?>(null) }
    val cameraFilePendente = cameraFilePath?.let { File(it) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        val f = cameraFilePendente
        if (res.resultCode == Activity.RESULT_OK && f != null && f.exists() && f.length() > 0) {
            setAnexoUi(AnexoUi.FotoTmp(f))
            val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", f)
            viewModel.processarOcr(uri, f.name, isFromSaf = false)
        } else {
            f?.delete()
        }
        cameraFilePath = null
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val dir = File(context.filesDir, FaturaRepository.PASTA_ANEXOS).apply { mkdirs() }
            val ficheiro = File(dir, "IMG_${System.currentTimeMillis()}.jpg")
            cameraFilePath = ficheiro.absolutePath
            val uri = FileProvider.getUriForFile(
                context, context.packageName + ".fileprovider", ficheiro
            )
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                .putExtra(MediaStore.EXTRA_OUTPUT, uri)
            runCatching { cameraLauncher.launch(intent) }.onFailure {
                ficheiro.delete()
                cameraFilePath = null
                erroGeral = context.getString(R.string.invoice_form_no_camera)
            }
        } else {
            erroGeral = context.getString(R.string.invoice_form_camera_denied)
        }
    }

    fun abrirCameraComPermissao() {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // --- SAF: PDF ou imagem existente ---
    val safLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val nome = runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
                }
            }.getOrNull().orEmpty().ifBlank { "anexo" }
            setAnexoUi(AnexoUi.Saf(uri, nome))
            viewModel.processarOcr(uri, nome, isFromSaf = true)
        }
    }

    Scaffold(
        topBar = {
            PremiumHeader(
                title = stringResource(
                    if (isEditing) R.string.invoice_form_edit_title
                    else R.string.invoice_form_new_title
                ),
                onNavigateBack = onNavigateBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            AnimatedVisibility(visible = detail.ocrLoading) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Text(
                        text = stringResource(R.string.invoice_form_ocr_reading),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            OutlinedTextField(
                value = fornecedor,
                onValueChange = {
                    fornecedor = it
                    erroFornecedor = null
                    preenchidoOcrFornecedor = false
                },
                label = { Text(stringResource(R.string.invoice_field_fornecedor)) },
                singleLine = true,
                isError = erroFornecedor != null,
                supportingText = {
                    if (erroFornecedor != null) {
                        Text(stringResource(erroFornecedor!!))
                    } else if (preenchidoOcrFornecedor) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.invoice_form_auto_filled), fontStyle = FontStyle.Italic, fontSize = 10.sp)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = numero,
                onValueChange = {
                    numero = it
                    erroNumero = null
                    preenchidoOcrNumero = false
                },
                label = { Text(stringResource(R.string.invoice_field_numero)) },
                singleLine = true,
                isError = erroNumero != null,
                supportingText = {
                    if (erroNumero != null) {
                        Text(stringResource(erroNumero!!))
                    } else if (preenchidoOcrNumero) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.invoice_form_auto_filled), fontStyle = FontStyle.Italic, fontSize = 10.sp)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Data de Emissão — DatePicker Material 3
            OutlinedButton(
                onClick = { mostrarPicker = true; erroData = null },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = dataSelecionada?.format(displayDateFormatter)
                        ?: stringResource(R.string.invoice_field_data_emissao),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (dataSelecionada == null) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface
                )
            }
            if (erroData != null) {
                Text(text = stringResource(erroData!!), color = Error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
            } else if (preenchidoOcrData) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp, top = 4.dp)) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.invoice_form_auto_filled), fontStyle = FontStyle.Italic, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (mostrarPicker) {
                val pickerState = androidx.compose.material3.rememberDatePickerState(
                    initialSelectedDateMillis = dataSelecionada
                        ?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
                )
                DatePickerDialog(
                    onDismissRequest = { mostrarPicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            pickerState.selectedDateMillis?.let { millis ->
                                setDataSelecionada(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                            }
                            preenchidoOcrData = false
                            mostrarPicker = false
                        }) { Text(stringResource(android.R.string.ok)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { mostrarPicker = false }) {
                            Text(stringResource(android.R.string.cancel))
                        }
                    }
                ) {
                    DatePicker(state = pickerState)
                }
            }

            // --- Anexo único: câmara OU ficheiro ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { abrirCameraComPermissao(); erroAnexo = null },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.invoice_form_attach_photo))
                }
                OutlinedButton(
                    onClick = { safLauncher.launch(arrayOf("application/pdf", "image/*")); erroAnexo = null },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.invoice_form_attach_file))
                }
            }
            erroAnexo?.let {
                Text(text = stringResource(it), color = Error, style = MaterialTheme.typography.bodySmall)
            }

            // Preview do anexo selecionado
            anexo?.let { a ->
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Box {
                        if (a.eImagem()) {
                            val model: Any = when (a) {
                                is AnexoUi.FotoTmp -> a.ficheiro
                                is AnexoUi.Saf -> a.uri
                                is AnexoUi.Existente -> File(a.caminho)
                            }
                            AsyncImage(
                                model = model,
                                contentDescription = a.nomeFicheiro(),
                                modifier = Modifier.fillMaxWidth().height(180.dp),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PictureAsPdf,
                                    contentDescription = null,
                                    tint = Error
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = a.nomeFicheiro(), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        IconButton(
                            onClick = {
                                when (a) {
                                    is AnexoUi.FotoTmp -> a.ficheiro.delete()
                                    else -> {}
                                }
                                setAnexoUi(null)
                                if (a is AnexoUi.Existente && a.caminho == anexoOriginalPath) {
                                    erroAnexo = R.string.invoice_form_error_attachment
                                }
                            },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.invoice_form_remove_attachment),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            erroGeral?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = it, color = Error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    erroFornecedor = null; erroNumero = null; erroData = null
                    erroAnexo = null; erroGeral = null

                    var invalido = false
                    if (fornecedor.isBlank()) {
                        erroFornecedor = R.string.invoice_form_error_fornecedor; invalido = true
                    }
                    if (numero.isBlank()) {
                        erroNumero = R.string.invoice_form_error_numero; invalido = true
                    }

                    val hoje = LocalDate.now()
                    val dataSel = dataSelecionada
                    if (dataSel == null) {
                        erroData = R.string.invoice_form_error_date_required; invalido = true
                    } else if (dataSel.isAfter(hoje)) {
                        erroData = R.string.invoice_form_error_date_future; invalido = true
                    }

                    val anexoSel = anexo
                    if (anexoSel == null) {
                        erroAnexo = R.string.invoice_form_error_attachment; invalido = true
                    }

                    if (!invalido && !isSaving && dataSel != null && anexoSel != null) {
                        scope.launch {
                            isSaving = true
                            runCatching {
                                val caminhoFinal = when (anexoSel) {
                                    is AnexoUi.FotoTmp -> anexoSel.ficheiro.absolutePath
                                    is AnexoUi.Saf -> viewModel.copiarAnexo(anexoSel.uri)
                                    is AnexoUi.Existente -> anexoSel.caminho
                                }
                                val antigo = anexoOriginalPath
                                if (isEditing && antigo != null && antigo != caminhoFinal) {
                                    viewModel.apagarAnexoInterno(antigo)
                                }
                                viewModel.salvarFatura(
                                    Fatura(
                                        id = faturaId ?: 0L,
                                        fornecedor = fornecedor.trim(),
                                        numeroFatura = numero.trim(),
                                        dataEmissao = dataSel
                                            .atStartOfDay(ZoneId.systemDefault())
                                            .toInstant().toEpochMilli(),
                                        caminhoAnexo = caminhoFinal,
                                        dataInsercao = detail.fatura?.dataInsercao
                                            ?: System.currentTimeMillis()
                                    ),
                                    isEditing = isEditing
                                )
                            }.onSuccess {
                                onSaved()
                            }.onFailure { e ->
                                erroGeral = e.message
                                isSaving = false
                            }
                        }
                    }
                },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(stringResource(R.string.invoice_form_save))
            }
        }
    }
}

