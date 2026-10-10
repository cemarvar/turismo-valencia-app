package com.cmarcillovargas.turismovalencia.data.model

/** Familias de páginas de la web (cada una es un módulo del repositorio `TurismoValencia`). */
enum class WebFamily(val key: String) {
    QueVer("quever"),
    Eventos("eventos"),
    Gastronomia("gastronomia"),
    Alojamientos("alojamientos"),
    Entradas("entradas"),
    Tours("tours"),
    Experiencias("experiencias"),
    Esencial("esencial"),
    Planes("planes"),
    Sostenible("sostenible"),
    Legal("legal"),
    ;

    companion object {
        fun fromKey(key: String): WebFamily = entries.firstOrNull { it.key == key } ?: QueVer
    }
}

/** Etiqueta de una tarjeta («Gratuito», «Familia», «Dic–Ene»…). */
data class WebTag(val label: String, val free: Boolean = false)

/** Elemento de una página: una sección numerada, un plan, un hotel, un paso, una pregunta… */
data class WebItem(
    val num: String? = null,
    val kicker: String? = null,
    val title: String? = null,
    val subtitle: String? = null,
    val body: String? = null,
    val facts: List<Pair<String, String>> = emptyList(),
    val bullets: List<String> = emptyList(),
    val bulletsTitle: String? = null,
    val notes: List<Pair<String, String>> = emptyList(),
    val tags: List<WebTag> = emptyList(),
    /** Ruta de la foto dentro de `assets/img/`. */
    val image: String? = null,
    val url: String? = null,
    /** Id del filtro al que pertenece, si el bloque tiene filtros. */
    val category: String? = null,
)

data class WebFilter(val id: String, val label: String)

/** Bloque de una página, en el orden en que aparece en la web. */
sealed interface WebBlock {
    val title: String

    /** Lista de elementos (con filtros opcionales). */
    data class Items(override val title: String, val filters: List<WebFilter>, val items: List<WebItem>) : WebBlock

    /** Pares «etiqueta · valor» (datos de la visita, datos útiles…). */
    data class Facts(override val title: String, val rows: List<Pair<String, String>>) : WebBlock

    /** Lista de consejos o puntos clave. */
    data class Bullets(override val title: String, val items: List<String>) : WebBlock
}

data class WebPage(
    val id: String,
    val family: WebFamily,
    val title: String,
    val eyebrow: String?,
    val lead: List<String>,
    val hero: String?,
    val blocks: List<WebBlock>,
)

/** Datos mínimos de una página para listas, buscador y tarjetas. */
data class WebPageMeta(
    val id: String,
    val family: WebFamily,
    val title: String,
    val eyebrow: String?,
    val hero: String?,
    val thumb: String?,
)

/** Tarjeta de una rejilla de la portada («Lo esencial», «Planes», «Turismo sostenible»). */
data class HomeTile(val image: String, val title: String, val route: String)

/** Índice de una sección de la web (Qué ver, Eventos, Gastronomía, Alojamientos): título, texto y fichas en su orden. */
data class CategoryIndex(val title: String, val subtitle: String, val tiles: List<HomeTile>)

data class WebIndex(
    val pages: Map<String, WebPageMeta>,
    val esencial: List<HomeTile>,
    val planes: List<HomeTile>,
    val sostenible: List<HomeTile>,
    val categories: Map<WebFamily, CategoryIndex>,
)
