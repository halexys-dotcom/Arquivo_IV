package pt.haconnect.arquivoiv.ui.fatura

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pt.haconnect.arquivoiv.R
import pt.haconnect.arquivoiv.domain.model.Fatura
import pt.haconnect.arquivoiv.ui.components.EmptyState
import pt.haconnect.arquivoiv.ui.components.FaturaCard
import pt.haconnect.arquivoiv.ui.components.PremiumHeader
import pt.haconnect.arquivoiv.ui.export.ExportViewModel
import pt.haconnect.arquivoiv.ui.theme.Primary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val searchDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault())

private fun fmtDia(millis: Long?): String =
    millis?.let { searchDateFormatter.format(Instant.ofEpochMilli(it)) } ?: ""

// Fase 4: pesquisa por fornecedor e/ou intervalo de datas de emissão.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaturaSearchScreen(
        onFaturaClick: (Long) -> Unit,
    onEditClick: (Long) -> Unit,
    viewModel: FaturaSearchViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val dataInicio by viewModel.dataInicio.collectAsStateWithLifecycle()
    val dataFim by viewModel.dataFim.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()

    var pickerAlvo by rememberSaveable { mutableStateOf<Int?>(null) } // 0 = "De", 1 = "Até"
    val temFiltros = query.isNotEmpty() || dataInicio != null || dataFim != null

    Scaffold(
        topBar = { PremiumHeader(title = stringResource(R.string.search_title)) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = viewModel::onQueryChanged,
                    label = { Text(stringResource(R.string.search_hint_fornecedor)) },
                    singleLine = true,
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onQueryChanged("") }) {
                                Icon(Icons.Filled.Close, contentDescription = null)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(onClick = { pickerAlvo = 0 }, modifier = Modifier.weight(1f)) {
                        Icon(
                            Icons.Filled.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = fmtDia(dataInicio).ifBlank { stringResource(R.string.search_de) },
                            color = if (dataInicio == null) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    OutlinedButton(onClick = { pickerAlvo = 1 }, modifier = Modifier.weight(1f)) {
                        Icon(
                            Icons.Filled.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = fmtDia(dataFim).ifBlank { stringResource(R.string.search_ate) },
                            color = if (dataFim == null) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (temFiltros || state.resultados.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { viewModel.limparFiltros() }) {
                            Text(stringResource(R.string.search_limpar_filtros), fontSize = 12.sp)
                        }
                    }
                }
            }

            if (state.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
            } else {
                items(state.resultados) { fatura ->
                    FaturaCard(
                        fatura = fatura,
                        onClick = { onFaturaClick(fatura.id) },
                        onEditClick = { onEditClick(fatura.id) }
                    )
                }
                if (state.resultados.isEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        EmptyState(
                            icon = Icons.Outlined.SearchOff,
                            title = stringResource(R.string.search_sem_resultados),
                            subtitle = stringResource(R.string.search_sem_resultados_hint)
                        )
                    }
                }
            }
        }
    }

    if (pickerAlvo != null) {
        val pickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = when (pickerAlvo) {
                0 -> dataInicio
                else -> dataFim
            }
        )
        DatePickerDialog(
            onDismissRequest = { pickerAlvo = null },
            confirmButton = {
                TextButton(onClick = {
                    val millis = pickerState.selectedDateMillis
                    if (pickerAlvo == 0) viewModel.setDataInicio(millis)
                    else viewModel.setDataFim(millis)
                    pickerAlvo = null
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { pickerAlvo = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}









