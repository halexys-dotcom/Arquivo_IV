package pt.haconnect.arquivoiv.ui.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import pt.haconnect.arquivoiv.domain.usecase.ExportarDadosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed class ExportUiState {
    data object Idle : ExportUiState()
    data object Exporting : ExportUiState()
    data class Success(val file: File, val format: String) : ExportUiState()
    data class Error(val message: String) : ExportUiState()
    data class Empty(val message: String) : ExportUiState()
}

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val exportarDadosUseCase: ExportarDadosUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<ExportUiState>(ExportUiState.Idle)
    val state: StateFlow<ExportUiState> = _state.asStateFlow()

        fun exportarPdf() {
        viewModelScope.launch {
            _state.value = ExportUiState.Exporting
            try {
                val file = exportarDadosUseCase.exportarPdf()
                if (file != null) {
                    _state.value = ExportUiState.Success(file, "PDF")
                    exportarDadosUseCase.shareFile(file)
                } else {
                    _state.value = ExportUiState.Empty("Empty")
                }
            } catch (e: Exception) {
                _state.value = ExportUiState.Error(e.message ?: "Error")
            }
        }
    }

    /** Exporta um subconjunto de faturas (ex.: resultado da pesquisa filtrada). */
    fun exportarPdf(faturas: List<pt.haconnect.arquivoiv.domain.model.Fatura>) {
        viewModelScope.launch {
            _state.value = ExportUiState.Exporting
            try {
                val file = exportarDadosUseCase.exportarPdf(faturas)
                if (file != null) {
                    _state.value = ExportUiState.Success(file, "PDF")
                    exportarDadosUseCase.shareFile(file)
                } else {
                    _state.value = ExportUiState.Empty("Empty")
                }
            } catch (e: Exception) {
                _state.value = ExportUiState.Error(e.message ?: "Error")
            }
        }
    }

    fun exportarCsv() {
        viewModelScope.launch {
            _state.value = ExportUiState.Exporting
            try {
                val file = exportarDadosUseCase.exportarCsv()
                if (file != null) {
                    _state.value = ExportUiState.Success(file, "CSV")
                    exportarDadosUseCase.shareFile(file)
                } else {
                    _state.value = ExportUiState.Empty("Empty")
                }
            } catch (e: Exception) {
                _state.value = ExportUiState.Error(e.message ?: "Error")
            }
        }
    }

    /** Exporta um subconjunto de faturas (ex.: resultado da pesquisa filtrada). */
    fun exportarCsv(faturas: List<pt.haconnect.arquivoiv.domain.model.Fatura>) {
        viewModelScope.launch {
            _state.value = ExportUiState.Exporting
            try {
                val file = exportarDadosUseCase.exportarCsv(faturas)
                if (file != null) {
                    _state.value = ExportUiState.Success(file, "CSV")
                    exportarDadosUseCase.shareFile(file)
                } else {
                    _state.value = ExportUiState.Empty("Empty")
                }
            } catch (e: Exception) {
                _state.value = ExportUiState.Error(e.message ?: "Error")
            }
        }
    }

    fun resetState() {
        _state.value = ExportUiState.Idle
    }
}









