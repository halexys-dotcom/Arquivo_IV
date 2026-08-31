package pt.haconnect.arquivoiv.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import pt.haconnect.arquivoiv.data.repository.FaturaRepository
import pt.haconnect.arquivoiv.domain.model.Fatura
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DashboardUiState(
    val totalFaturas: Int = 0,
    val totalFornecedores: Int = 0,
    val nomesFornecedores: List<String> = emptyList(),
    val inseridasUltimos30Dias: Int = 0,
    val ultimasFaturas: List<Fatura> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val faturaRepository: FaturaRepository
) : ViewModel() {

    // Utilizar stateIn para garantir que o dashboard observa as mudanças em tempo real (Fase 9.1)
    val state: StateFlow<DashboardUiState> = combine(
        faturaRepository.getAll(),
        faturaRepository.getFornecedores()
    ) { faturas, fornecedores ->
        val trintaDiasMillis = 30L * 24 * 60 * 60 * 1000
        val limite = System.currentTimeMillis() - trintaDiasMillis
        
        DashboardUiState(
            totalFaturas = faturas.size,
            totalFornecedores = fornecedores.size,
            nomesFornecedores = fornecedores,
            inseridasUltimos30Dias = faturas.count { it.dataInsercao >= limite },
            ultimasFaturas = faturas.sortedByDescending { it.dataInsercao }.take(5),
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true)
    )
}

