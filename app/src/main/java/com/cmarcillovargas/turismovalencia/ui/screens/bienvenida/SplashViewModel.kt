package com.cmarcillovargas.turismovalencia.ui.screens.bienvenida

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SplashUiState(
    val appVersion: String,
)

class SplashViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(SplashUiState(appVersion = readVersionName()))
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    private fun readVersionName(): String {
        val app = getApplication<Application>()
        val info = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                app.packageManager.getPackageInfo(app.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                app.packageManager.getPackageInfo(app.packageName, 0)
            }
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
        return info?.versionName.orEmpty()
    }
}
