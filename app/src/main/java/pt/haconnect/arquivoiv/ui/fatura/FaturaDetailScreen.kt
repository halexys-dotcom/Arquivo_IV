package pt.haconnect.arquivoiv.ui.fatura

import android.content.Context
import android.content.Intent
import android.webkit.MimeTypeMap
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pt.haconnect.arquivoiv.R
import pt.haconnect.arquivoiv.ui.components.EmptyState
import pt.haconnect.arquivoiv.ui.components.PremiumHeader
import pt.haconnect.arquivoiv.ui.theme.Error
import pt.haconnect.arquivoiv.ui.theme.Primary
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val detailDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault())

private fun formatar(timestamp: Long): String =
    detailDateFormatter.format(Instant.ofEpochMilli(timestamp))

private fun abrirAnexo(context: Context, caminho: String) {
    val ficheiro = File(caminho)
    if (!ficheiro.exists()) return
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", ficheiro)
    val mime = MimeTypeMap.getSingleton()
        .getMimeTypeFromExtension(ficheiro.extension.lowercase()) ?: "*/*"
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, mime)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaturaDetailScreen(
    faturaId: Long,
    onNavigateBack: () -> Unit,
    onEditClick: (Long) -> Unit,
    viewModel: FaturaViewModel = hiltViewModel()
) {
    val state by viewModel.detailState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmarEliminacao by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(faturaId) { viewModel.loadFatura(faturaId) }

    LaunchedEffect(state.deletado) {
        if (state.deletado) onNavigateBack()
    }

    Scaffold(
        topBar = {
            PremiumHeader(
                title = stringResource(R.string.invoice_detail_title),
                onNavigateBack = onNavigateBack
            )
        },
        bottomBar = {
            if (state.fatura != null) {
                val fatura = state.fatura!!
                Column {
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { onEditClick(fatura.id) },
                            modifier = Modifier.weight(1f),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.action_edit),
                                color = Primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        TextButton(
                            onClick = { viewModel.partilharFatura(fatura) },
                            modifier = Modifier.weight(1f),
                            enabled = !state.isSharing,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.export_btn_share),
                                color = Primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        TextButton(
                            onClick = { confirmarEliminacao = true },
                            modifier = Modifier.weight(1f),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null, tint = Error, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.action_delete),
                                color = Error,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            }

            state.fatura == null -> {
                EmptyState(
                    icon = Icons.Filled.Inbox,
                    title = stringResource(R.string.invoice_detail_not_found),
                    modifier = Modifier.padding(padding)
                )
            }

            else -> {
                val fatura = state.fatura!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoRow(stringResource(R.string.invoice_field_fornecedor), fatura.fornecedor)
                    InfoRow(stringResource(R.string.invoice_field_numero), fatura.numeroFatura)
                    InfoRow(
                        stringResource(R.string.invoice_field_data_emissao),
                        formatar(fatura.dataEmissao)
                    )
                    InfoRow(
                        stringResource(R.string.invoice_detail_archived_on),
                        formatar(fatura.dataInsercao)
                    )

                    HorizontalDivider()

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.invoice_field_anexo).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                            Text(
                                text = File(fatura.caminhoAnexo).name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            TextButton(onClick = { abrirAnexo(context, fatura.caminhoAnexo) }) {
                                Text(stringResource(R.string.invoice_open_attachment))
                            }
                        }
                    }
                }

                if (confirmarEliminacao) {
                    AlertDialog(
                        onDismissRequest = { confirmarEliminacao = false },
                        title = { Text(stringResource(R.string.action_delete)) },
                        text = { Text(stringResource(R.string.invoice_delete_confirm)) },
                        confirmButton = {
                            TextButton(onClick = {
                                confirmarEliminacao = false
                                viewModel.eliminarFatura(fatura)
                            }) {
                                Text(stringResource(R.string.action_delete), color = Error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { confirmarEliminacao = false }) {
                                Text(stringResource(R.string.action_cancel))
                            }
                        }
                    )
                }

                if (state.isSharing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = Primary)
                                Spacer(Modifier.height(16.dp))
                                Text(stringResource(R.string.invoice_form_ocr_reading)) // Reaproveitando string de leitura
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}








