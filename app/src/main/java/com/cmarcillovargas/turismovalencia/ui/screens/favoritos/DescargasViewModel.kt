package com.cmarcillovargas.turismovalencia.ui.screens.favoritos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmarcillovargas.turismovalencia.data.AppContainer
import com.cmarcillovargas.turismovalencia.data.model.ContentItem
import com.cmarcillovargas.turismovalencia.data.repo.DownloadsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DescargasUiState(
    val items: List<ContentItem> = emptyList(),
    val mapMb: Int = DownloadsRepository.MAP_MB,
    val cardsMb: Int = 0,
    val quotaMb: Int = DownloadsRepository.QUOTA_MB,
    val mapDownloaded: Boolean = true,
    val wifiOnly: Boolean = true,
    val autoUpdate: Boolean = true,
    val updating: Boolean = false,
) {
    val usedMb: Int get() = (if (mapDownloaded) mapMb else 0) + cardsMb
}

class DescargasViewModel(private val container: AppContainer) : ViewModel() {
    private val updating = MutableStateFlow(false)

    private val settings = combine(container.downloads.wifiOnly, container.downloads.autoUpdate, container.downloads.mapDownloaded, updating) { wifi, auto, map, busy ->
        listOf(wifi, auto, map, busy)
    }

    val uiState: StateFlow<DescargasUiState> = combine(container.downloads.ids, settings) { ids, flags ->
        DescargasUiState(
            items = container.content.items(ids),
            mapMb = DownloadsRepository.MAP_MB,
            cardsMb = container.downloads.cardsMb(ids),
            mapDownloaded = flags[2],
            wifiOnly = flags[0],
            autoUpdate = flags[1],
            updating = flags[3],
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DescargasUiState())

    fun onWifiOnly(value: Boolean) = container.downloads.setWifiOnly(value)

    fun onAutoUpdate(value: Boolean) = container.downloads.setAutoUpdate(value)

    fun onRemove(id: String) = container.downloads.remove(id)

    fun onRemoveAll() = container.downloads.removeAll()

    /** Simula la actualización de las fichas guardadas. */
    // TODO(api): refrescar las fichas guardadas desde el backend (WorkManager de noche si «actualizar automáticamente»).
    fun onUpdate() {
        viewModelScope.launch {
            updating.value = true
            delay(UpdateMillis)
            updating.value = false
        }
    }

    private companion object {
        const val UpdateMillis = 1_200L
    }
}
