package com.cmarcillovargas.turismovalencia.data.repo

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Estado real de la conexión a Internet. Mientras haya contenido de ejemplo, la app sigue funcionando sin red. */
class NetworkMonitor(context: Context) {
    private val manager = context.getSystemService(ConnectivityManager::class.java)
    private var real = currentlyOnline()
    private var forced: Boolean? = null

    private val _isOnline = MutableStateFlow(real)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        manager?.registerDefaultNetworkCallback(
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) = update(true)

                override fun onLost(network: Network) = update(false)

                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) =
                    update(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            },
        )
    }

    /** «Reintentar»: vuelve a comprobar la conexión. */
    fun retry() = update(currentlyOnline())

    /** Solo para pruebas de las pantallas sin conexión (`forced` = null devuelve el estado real). */
    fun force(online: Boolean?) {
        forced = online
        _isOnline.value = forced ?: real
    }

    private fun update(online: Boolean) {
        real = online
        _isOnline.value = forced ?: real
    }

    private fun currentlyOnline(): Boolean {
        val caps = manager?.getNetworkCapabilities(manager.activeNetwork)
        return caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
}
