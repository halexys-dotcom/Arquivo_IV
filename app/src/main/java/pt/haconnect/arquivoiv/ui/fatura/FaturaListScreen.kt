package pt.haconnect.arquivoiv.ui.fatura

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pt.haconnect.arquivoiv.R
import pt.haconnect.arquivoiv.ui.components.EmptyState
import pt.haconnect.arquivoiv.ui.components.FaturaCard
import pt.haconnect.arquivoiv.ui.components.PremiumHeader
import pt.haconnect.arquivoiv.ui.theme.Primary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaturaListScreen(
    onFaturaClick: (Long) -> Unit,
    onEditClick: (Long) -> Unit,
    viewModel: FaturaViewModel = hiltViewModel()
) {
    val state by viewModel.listState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            PremiumHeader(title = stringResource(R.string.invoices_title))
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.faturas.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Filled.Inbox,
                            title = stringResource(R.string.invoices_empty_title),
                            subtitle = stringResource(R.string.invoices_empty_subtitle)
                        )
                    }
                } else {
                    items(state.faturas) { fatura ->
                        FaturaCard(
                            fatura = fatura,
                            onClick = { onFaturaClick(fatura.id) },
                            onEditClick = { onEditClick(fatura.id) }
                        )
                    }
                }
            }
        }
    }
}








