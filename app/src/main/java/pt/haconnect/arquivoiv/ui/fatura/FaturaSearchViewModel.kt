package pt.haconnect.arquivoiv.ui.fatura

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import pt.haconnect.arquivoiv.data.repository.FaturaRepository
import pt.haconnect.arquivoiv.domain.model.Fatura
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

data class FaturaSearchUiState(
    val resultados: List<Fatura> = emptyList(),
    val isLoading: Boolean = true
)

/** Fase 4: filtros combinados (fornecedor contains + intervalo de datas), debounce 300 ms. */
@HiltViewModel
class FaturaSearchViewModel @Inject constructor(
    private val faturaRepository: FaturaRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _dataInicio = MutableStateFlow<Long?>(null)
    val dataInicio: StateFlow<Long?> = _dataInicio.asStateFlow()

    private val _dataFim = MutableStateFlow<Long?>(null)
    val dataFim: StateFlow<Long?> = _dataFim.asStateFlow()

    private val _state = MutableStateFlow(FaturaSearchUiState())
    val state: StateFlow<FaturaSearchUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            combine(_query, _dataInicio, _dataFim) { q, inicio, fim -> Triple(q, inicio, fim) }
                .collect { (q, inicio, fim) ->
                    searchJob?.cancel()
                    searchJob = launch {
                        delay(300) // debounce: evita query por cada tecla
                        faturaRepository.pesquisar(q, inicio, fim).collect { resultados ->
                            _state.value = FaturaSearchUiState(
                                resultados = resultados,
                                isLoading = false
                            )
                        }
                    }
                }
        }
    }

    fun onQueryChanged(value: String) {
        _query.value = value
    }

    /** Início do intervalo — normalizado para o início do dia local. */
    fun setDataInicio(diaUtcMillis: Long?) {
        _dataInicio.value = diaUtcMillis?.let {
            Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
    }

    /** Fim do intervalo — normalizado para o fim do dia local (23:59:59.999). */
    fun setDataFim(diaUtcMillis: Long?) {
        _dataFim.value = diaUtcMillis?.let {
            Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                .atTime(23, 59, 59, 999_000_000)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
    }

    fun limparFiltros() {
        _query.value = ""
        _dataInicio.value = null
        _dataFim.value = null
    }
}








