package com.cmarcillovargas.turismovalencia.data.remote

import java.io.File

/**
 * Los ejemplos de `backend/contract/examples/` son el contrato de la API (salen de respuestas reales y el servidor comprueba que
 * sigue respondiendo con esa forma). Las pruebas de la app los leen tal cual, para que un cambio incompatible en cualquiera de
 * los dos lados falle aquí.
 */
internal fun contractExample(name: String): String {
    // Gradle ejecuta las pruebas con la carpeta del módulo (`app/`) como directorio de trabajo.
    val candidates = listOf(File("../backend/contract/examples/$name"), File("backend/contract/examples/$name"))
    val file = candidates.firstOrNull { it.isFile } ?: error("No se encuentra el ejemplo del contrato «$name» (buscado en ${candidates.map { it.absolutePath }})")
    return file.readText(Charsets.UTF_8)
}
