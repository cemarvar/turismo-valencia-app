package com.cmarcillovargas.turismovalencia.navigation

import android.net.Uri

/** Rutas de la app. Las pestañas de la barra inferior no llevan argumentos. */
object Routes {
    const val Inicio = "inicio"
    const val Mapa = "mapa"
    const val Valbot = "valbot"
    const val Favoritos = "favoritos"
    const val Reservas = "reservas"

    const val Busqueda = "busqueda"
    const val Sostenible = "sostenible"
    const val Descargas = "descargas"
    const val Perfil = "perfil"
    const val Notificaciones = "notificaciones"

    // Patrones con argumentos.
    const val MapaPattern = "mapa?focus={focus}&q={q}"
    const val ValbotPattern = "valbot?q={q}"
    const val CategoriaPattern = "categoria/{key}"
    const val DetallePattern = "detalle/{id}"
    const val EntradasPattern = "entradas?tab={tab}"
    const val RutaPattern = "ruta/{id}"
    const val ReservarPattern = "reservar/{offerId}"
    const val ReservaPattern = "reserva/{id}"

    fun mapa(focus: String? = null, query: String? = null) = "mapa?focus=${Uri.encode(focus.orEmpty())}&q=${Uri.encode(query.orEmpty())}"

    fun valbot(question: String? = null) = "valbot?q=${Uri.encode(question.orEmpty())}"

    fun categoria(key: String) = "categoria/$key"

    fun detalle(id: String) = "detalle/$id"

    fun entradas(tab: String? = null) = "entradas?tab=${tab.orEmpty()}"

    fun ruta(id: String) = "ruta/$id"

    fun reservar(offerId: String) = "reservar/$offerId"

    fun reserva(id: String) = "reserva/$id"

    /** Pestaña de la barra inferior a la que pertenece una ruta, o `null` si la pantalla no muestra la barra. */
    fun tabOf(route: String?): String? = when {
        route == null -> null
        route.startsWith("inicio") || route.startsWith("categoria") || route.startsWith("entradas") || route.startsWith("sostenible") -> Inicio
        route.startsWith("mapa") -> Mapa
        route.startsWith("valbot") -> Valbot
        route.startsWith("favoritos") -> Favoritos
        route.startsWith("reservas") -> Reservas
        else -> null
    }
}
