package pt.haconnect.arquivoiv.ui.export

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pt.haconnect.arquivoiv.R
import pt.haconnect.arquivoiv.ui.components.PremiumHeader
import pt.haconnect.arquivoiv.ui.theme.Error
import pt.haconnect.arquivoiv.ui.theme.Primary
import pt.haconnect.arquivoiv.ui.theme.PrimaryLight
import pt.haconnect.arquivoiv.ui.theme.RetentionOk

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    onNavigateBack: () -> Unit,
    viewModel: ExportViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val successMessage = stringResource(R.string.export_success, (state as? ExportUiState.Success)?.format ?: "")
    val emptyMessage = stringResource(R.string.export_empty)

    LaunchedEffect(state) {
        when (state) {
            is ExportUiState.Success -> {
                snackbarHostState.showSnackbar(successMessage)
                viewModel.resetState()
            }
            is ExportUiState.Error -> {
                snackbarHostState.showSnackbar((state as ExportUiState.Error).message)
                viewModel.resetState()
            }
            is ExportUiState.Empty -> {
                snackbarHostState.showSnackbar(emptyMessage)
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            PremiumHeader(
                title = stringResource(R.string.export_title),
                onNavigateBack = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(32.dp))
            Icon(imageVector = Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(64.dp), tint = Primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = stringResource(R.string.export_heading), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = stringResource(R.string.export_description), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(32.dp))

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = PrimaryLight.copy(alpha = 0.1f)), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(40.dp), tint = Error)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = stringResource(R.string.export_pdf_card_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = stringResource(R.string.export_pdf_card_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.exportarPdf() }, modifier = Modifier.fillMaxWidth().height(52.dp), enabled = state !is ExportUiState.Exporting, colors = ButtonDefaults.buttonColors(containerColor = Error), shape = RoundedCornerShape(12.dp)) {
                        if (state is ExportUiState.Exporting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        else { Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.width(8.dp)); Text(stringResource(R.string.export_btn_share), color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = RetentionOk.copy(alpha = 0.1f)), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.TableChart, contentDescription = null, modifier = Modifier.size(40.dp), tint = RetentionOk)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = stringResource(R.string.export_csv_card_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = stringResource(R.string.export_csv_card_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.exportarCsv() }, modifier = Modifier.fillMaxWidth().height(52.dp), enabled = state !is ExportUiState.Exporting, colors = ButtonDefaults.buttonColors(containerColor = RetentionOk), shape = RoundedCornerShape(12.dp)) {
                        if (state is ExportUiState.Exporting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        else { Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.width(8.dp)); Text(stringResource(R.string.export_btn_share), color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = RetentionOk)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = stringResource(R.string.export_info_title), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(text = stringResource(R.string.export_info_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}








