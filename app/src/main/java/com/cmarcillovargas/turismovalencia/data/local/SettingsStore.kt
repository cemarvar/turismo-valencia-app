package com.cmarcillovargas.turismovalencia.data.local

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/** Fila de ajustes de la base de datos vista como estado, con un `StateFlow` por ajuste y una escritura atómica. */
class SettingsStore(private val dao: SettingsDao, private val scope: CoroutineScope) {
    private val row: StateFlow<SettingsEntity> = dao.observe().map { it ?: SettingsEntity() }.asState(scope)

    /** Estado de un ajuste concreto; solo emite cuando ese ajuste cambia. */
    fun <T> select(read: (SettingsEntity) -> T): StateFlow<T> =
        row.map(read).stateIn(scope, SharingStarted.Eagerly, read(row.value))

    fun update(change: (SettingsEntity) -> SettingsEntity) {
        scope.launch { dao.update(change) }
    }

    /** El identificador anónimo de este móvil para la API (un UUID): se crea la primera vez que se pide y ya no cambia. */
    suspend fun deviceId(): String = dao.deviceId { UUID.randomUUID().toString() }

    /** El identificador de este móvil si ya se creó (es decir, si alguna vez ha reservado); no lo crea. */
    suspend fun deviceIdOrNull(): String? = dao.current()?.deviceId
}
