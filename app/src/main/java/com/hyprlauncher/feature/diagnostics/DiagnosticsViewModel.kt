package com.hyprlauncher.feature.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyprlauncher.data.repository.DiagnosticsRepository
import com.hyprlauncher.domain.model.DiagnosticReport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiagnosticsUiState(
    val report: DiagnosticReport? = null,
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
    val exportedContent: String? = null,
    val autoRefresh: Boolean = true
)

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val diagnosticsRepository: DiagnosticsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiagnosticsUiState(isLoading = true))
    val uiState: StateFlow<DiagnosticsUiState> = _uiState.asStateFlow()
    private var tickerJob: Job? = null

    init {
        // Initial snapshot
        refresh()
    }

    fun startPolling() {
        if (tickerJob != null) return
        tickerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                if (_uiState.value.autoRefresh) {
                    val current = runCatching { diagnosticsRepository.getLiveReport() }.getOrNull()
                    if (current != null) {
                        _uiState.update { it.copy(report = current, isLoading = false) }
                    }
                }
            }
        }
    }

    fun stopPolling() {
        tickerJob?.cancel()
        tickerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val report = runCatching { diagnosticsRepository.getLiveReport() }.getOrNull()
            _uiState.update { it.copy(report = report, isLoading = false) }
        }
    }

    fun toggleAutoRefresh() {
        _uiState.update { it.copy(autoRefresh = !it.autoRefresh) }
    }

    fun exportDiagnostics(asJson: Boolean) {
        viewModelScope.launch {
            val result = diagnosticsRepository.exportDiagnostics(asJson)
            result.onSuccess { content ->
                _uiState.update {
                    it.copy(
                        exportedContent = content,
                        statusMessage = if (asJson) "Diagnostics exported as JSON" else "Diagnostics exported as ASCII"
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(statusMessage = "Export failed: ${err.message}") }
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            diagnosticsRepository.clearCache()
            refresh()
            _uiState.update { it.copy(statusMessage = "Icon memory & disk cache cleared") }
        }
    }

    fun rebuildAppIndex() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            diagnosticsRepository.rebuildAppIndex()
            refresh()
            _uiState.update { it.copy(statusMessage = "Application index rebuilt successfully") }
        }
    }

    fun resetUiState() {
        viewModelScope.launch {
            diagnosticsRepository.resetUiState()
            refresh()
            _uiState.update { it.copy(statusMessage = "UI frame and latency metrics reset") }
        }
    }

    fun dismissExport() {
        _uiState.update { it.copy(exportedContent = null) }
    }

    fun dismissStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}
