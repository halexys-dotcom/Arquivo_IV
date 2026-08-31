package pt.haconnect.arquivoiv.ui.update

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pt.haconnect.arquivoiv.domain.usecase.UpdateInfo
import pt.haconnect.arquivoiv.domain.usecase.UpdateUseCase
import java.io.File
import javax.inject.Inject

data class UpdateUiState(
    val installedVersionName: String = "",
    val isChecking: Boolean = false,
    val updateInfo: UpdateInfo? = null,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val apkFile: File? = null,
    val isReadyToInstall: Boolean = false,
    val mensagemErro: String? = null,
    val statusMensagem: String? = null
)

@HiltViewModel
class UpdateViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val updateUseCase: UpdateUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(UpdateUiState())
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    init {
        obterVersaoInstalada()
        verificarAtualizacao()
    }

    private fun obterVersaoInstalada() {
        val version = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
        _state.value = _state.value.copy(installedVersionName = version)
    }

    fun verificarAtualizacao() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isChecking = true, mensagemErro = null)
            val info = updateUseCase.verificarAtualizacao(_state.value.installedVersionName)
            _state.value = _state.value.copy(
                isChecking = false,
                updateInfo = info,
                mensagemErro = if (info.isError && info.errorMessage != null) info.errorMessage else null
            )
        }
    }

    fun descarregarEInstalar() {
        val info = _state.value.updateInfo ?: return
        if (info.apkUrl.isBlank()) return

        viewModelScope.launch {
            _state.value = _state.value.copy(
                isDownloading = true,
                downloadProgress = 0f,
                mensagemErro = null
            )

            val file = updateUseCase.descarregarApk(
                apkUrl = info.apkUrl,
                expectedSizeBytes = info.apkSizeBytes,
                onProgress = { downloaded, total ->
                    val progress = if (total > 0) downloaded.toFloat() / total.toFloat() else 0f
                    _state.value = _state.value.copy(downloadProgress = progress)
                }
            )

            if (file != null && file.exists()) {
                _state.value = _state.value.copy(
                    isDownloading = false,
                    downloadProgress = 1f,
                    apkFile = file,
                    isReadyToInstall = true,
                    statusMensagem = "Download concluído com sucesso."
                )
                instalarApk()
            } else {
                _state.value = _state.value.copy(
                    isDownloading = false,
                    mensagemErro = "Erro ao descarregar a atualização. Verifique a sua ligação."
                )
            }
        }
    }

    fun instalarApk() {
        val file = _state.value.apkFile ?: return
        if (file.exists()) {
            updateUseCase.abrirInstalador(file)
        }
    }

    fun abrirGitHub() {
        updateUseCase.abrirGitHubNoBrowser()
    }

    fun limparMensagens() {
        _state.value = _state.value.copy(mensagemErro = null, statusMensagem = null)
    }
}
