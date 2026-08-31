package pt.haconnect.arquivoiv.ui.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pt.haconnect.arquivoiv.domain.usecase.BackupInfo
import pt.haconnect.arquivoiv.domain.usecase.BackupResult
import pt.haconnect.arquivoiv.domain.usecase.BackupUseCase
import java.io.File
import javax.inject.Inject

data class BackupUiState(
    val backups: List<BackupInfo> = emptyList(),
    val espacoTotalFormatted: String = "0 B",
    val isLoading: Boolean = true,
    val isCreating: Boolean = false,
    val isRestoring: Boolean = false,
    val mensagemStatus: String? = null,
    val erroMensagem: String? = null,
    val backupRestauradoSucesso: Boolean = false
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupUseCase: BackupUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    init {
        carregarBackups()
    }

    fun carregarBackups() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val lista = backupUseCase.listarBackups()
            val espaco = backupUseCase.calcularEspacoTotal(lista)
            _state.value = _state.value.copy(
                backups = lista,
                espacoTotalFormatted = espaco,
                isLoading = false
            )
        }
    }

    fun criarBackup() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isCreating = true, erroMensagem = null, mensagemStatus = null)
            val res = backupUseCase.criarBackup()
            when (res) {
                is BackupResult.Success -> {
                    _state.value = _state.value.copy(
                        isCreating = false,
                        mensagemStatus = "Backup criado com sucesso!"
                    )
                    carregarBackups()
                }
                is BackupResult.Error -> {
                    _state.value = _state.value.copy(
                        isCreating = false,
                        erroMensagem = res.error
                    )
                }
            }
        }
    }

    fun importarBackup(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, erroMensagem = null, mensagemStatus = null)
            val res = backupUseCase.importarBackup(uri)
            when (res) {
                is BackupResult.Success -> {
                    _state.value = _state.value.copy(
                        mensagemStatus = "Backup importado com sucesso!"
                    )
                    carregarBackups()
                }
                is BackupResult.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        erroMensagem = res.error
                    )
                }
            }
        }
    }

    fun restaurarBackup(info: BackupInfo) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRestoring = true, erroMensagem = null, mensagemStatus = null)
            val res = backupUseCase.restaurarBackup(info)
            when (res) {
                is BackupResult.Success -> {
                    _state.value = _state.value.copy(
                        isRestoring = false,
                        backupRestauradoSucesso = true
                    )
                }
                is BackupResult.Error -> {
                    _state.value = _state.value.copy(
                        isRestoring = false,
                        erroMensagem = res.error
                    )
                }
            }
        }
    }

    fun eliminarBackup(file: File) {
        viewModelScope.launch {
            backupUseCase.eliminarBackup(file)
            carregarBackups()
        }
    }

    fun partilharBackup(file: File) {
        backupUseCase.partilharBackup(file)
    }

    fun limparMensagens() {
        _state.value = _state.value.copy(mensagemStatus = null, erroMensagem = null)
    }
}
