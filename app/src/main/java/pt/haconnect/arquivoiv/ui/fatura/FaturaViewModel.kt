package pt.haconnect.arquivoiv.ui.fatura

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import pt.haconnect.arquivoiv.data.repository.FaturaRepository
import pt.haconnect.arquivoiv.domain.model.Fatura
import pt.haconnect.arquivoiv.domain.usecase.ExportarDadosUseCase
import pt.haconnect.arquivoiv.domain.usecase.OcrExtrairDadosUseCase
import pt.haconnect.arquivoiv.domain.usecase.OcrResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FaturaListUiState(
    val faturas: List<Fatura> = emptyList(),
    val isLoading: Boolean = true
)

data class FaturaDetailUiState(
    val fatura: Fatura? = null,
    val isLoading: Boolean = true,
    val isDeleting: Boolean = false,
    val isSharing: Boolean = false,
    val deletado: Boolean = false,
    val ocrLoading: Boolean = false,
    val ocrResult: OcrResult? = null
)

@HiltViewModel
class FaturaViewModel @Inject constructor(
    private val faturaRepository: FaturaRepository,
    private val ocrUseCase: OcrExtrairDadosUseCase,
    private val exportarDadosUseCase: ExportarDadosUseCase
) : ViewModel() {

    private val _listState = MutableStateFlow(FaturaListUiState())
    val listState: StateFlow<FaturaListUiState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow(FaturaDetailUiState())
    val detailState: StateFlow<FaturaDetailUiState> = _detailState.asStateFlow()

    private val _fornecedores = MutableStateFlow<List<String>>(emptyList())

    init {
        loadFaturas()
        observeFornecedores()
    }

    private fun observeFornecedores() {
        viewModelScope.launch {
            faturaRepository.getFornecedores().collect {
                _fornecedores.value = it
            }
        }
    }

    fun processarOcr(uri: Uri, nomeFicheiro: String? = null, isFromSaf: Boolean = false) {
        viewModelScope.launch {
            _detailState.value = _detailState.value.copy(ocrLoading = true, ocrResult = null)
            try {
                val res = ocrUseCase.extrair(uri, _fornecedores.value, nomeFicheiro, isFromSaf)
                _detailState.value = _detailState.value.copy(ocrLoading = false, ocrResult = res)
            } catch (e: Exception) {
                _detailState.value = _detailState.value.copy(ocrLoading = false)
            }
        }
    }

    fun clearOcrResult() {
        _detailState.value = _detailState.value.copy(ocrResult = null)
    }

    fun partilharFatura(fatura: Fatura) {
        viewModelScope.launch {
            _detailState.value = _detailState.value.copy(isSharing = true)
            try {
                val file = exportarDadosUseCase.exportarFaturaIndividual(fatura)
                if (file != null) {
                    exportarDadosUseCase.shareFile(file)
                }
            } finally {
                _detailState.value = _detailState.value.copy(isSharing = false)
            }
        }
    }

    private fun loadFaturas() {
        viewModelScope.launch {
            _listState.value = _listState.value.copy(isLoading = true)
            faturaRepository.getAll().collect { faturas ->
                _listState.value = FaturaListUiState(
                    faturas = faturas,
                    isLoading = false
                )
            }
        }
    }

    fun loadFatura(id: Long) {
        viewModelScope.launch {
            _detailState.value = FaturaDetailUiState(isLoading = true)
            val fatura = faturaRepository.getById(id)
            _detailState.value = FaturaDetailUiState(
                fatura = fatura,
                isLoading = false
            )
        }
    }

    suspend fun salvarFatura(fatura: Fatura, isEditing: Boolean) {
        if (isEditing) {
            faturaRepository.update(fatura)
        } else {
            faturaRepository.insert(fatura)
        }
    }

    fun eliminarFatura(fatura: Fatura) {
        viewModelScope.launch {
            _detailState.value = _detailState.value.copy(isDeleting = true)
            faturaRepository.delete(fatura)
            _detailState.value = _detailState.value.copy(
                isDeleting = false,
                deletado = true
            )
        }
    }

    /** Copia o anexo escolhido para o armazenamento interno (delega no repositório). */
    suspend fun copiarAnexo(uri: Uri): String = faturaRepository.copiarAnexo(uri)

    /** Remove anexo interno órfão quando substituído numa edição. */
    fun apagarAnexoInterno(caminho: String) = faturaRepository.apagarAnexoSeInterno(caminho)
}

