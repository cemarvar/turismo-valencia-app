package com.cmarcillovargas.turismovalencia.data.local

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Fila de ajustes de la base de datos vista como estado, con un `StateFlow` por ajuste y una escritura atómica. */
class SettingsStore(private val dao: SettingsDao, private val scope: CoroutineScope) {
    private val row: StateFlow<SettingsEntity> = dao.observe().map { it ?: SettingsEntity() }.asState(scope)

    /** Estado de un ajuste concreto; solo emite cuando ese ajuste cambia. */
    fun <T> select(read: (SettingsEntity) -> T): StateFlow<T> =
        row.map(read).stateIn(scope, SharingStarted.Eagerly, read(row.value))

    fun update(change: (SettingsEntity) -> SettingsEntity) {
        scope.launch { dao.update(change) }
    }
}
