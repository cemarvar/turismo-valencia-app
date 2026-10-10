package com.cmarcillovargas.turismovalencia.data.model

import androidx.annotation.DrawableRes

/** Secciones de contenido de la web: Qué ver, Gastronomía, Alojamientos, Eventos y Turismo sostenible. */
enum class Category(val key: String) {
    QueVer("que-ver"),
    Gastronomia("gastronomia"),
    Alojamientos("alojamientos"),
    Eventos("eventos"),
    Sostenible("sostenible"),
    ;

    companion object {
        fun fromKey(key: String?): Category = entries.firstOrNull { it.key == key } ?: QueVer
    }
}

data class Stat(val value: String, val label: String)

enum class InfoKind { Schedule, Address, Price, Transport, Tip }

data class InfoRow(val kind: InfoKind, val title: String, val text: String)

/** Punto del mapa dibujado, en dp sobre un lienzo de 700×1100. */
data class MapPoint(val x: Float, val y: Float)

/** Acto de un evento («Mascletà», «Plantà»…). */
data class Act(val title: String, val whenLabel: String, @param:DrawableRes val image: Int? = null)

/** Ficha de contenido: lugares, platos, zonas de alojamiento, eventos y temas sostenibles. */
data class ContentItem(
    val id: String,
    val category: Category,
    /** Filtro de la lista («Monumentos», «Museos», «Barrios», «Naturaleza»…). */
    val subcategory: String,
    /** Etiqueta superior, se muestra en mayúsculas («Monumento · UNESCO»). */
    val eyebrow: String,
    val title: String,
    val area: String,
    val distanceMeters: Int,
    val rating: Double?,
    val ratingCount: String?,
    @param:DrawableRes val thumb: Int?,
    @param:DrawableRes val hero: Int?,
    val summary: String,
    val description: String,
    val mapPoint: MapPoint,
    val gallery: List<Int> = emptyList(),
    val stats: List<Stat> = emptyList(),
    val info: List<InfoRow> = emptyList(),
    val isFree: Boolean = false,
    val isEco: Boolean = false,
    val offlineMb: Int = 5,
    val acts: List<Act> = emptyList(),
    val dateLabel: String? = null,
    val openUntil: String? = null,
    val keywords: List<String> = emptyList(),
    /** Título de la ficha cuando difiere del de las listas («Fallas 2027» → «Les Falles de València»). */
    val detailTitle: String? = null,
    /** Foto apaisada del carrusel de destacados del inicio. */
    @param:DrawableRes val feature: Int? = null,
    val featureLabel: String? = null,
    val featureSubtitle: String? = null,
    /** Etiqueta de la cabecera de la ficha cuando difiere de la de las listas. */
    val detailEyebrow: String? = null,
    /** Categoría con la que se dibuja el marcador del mapa, si no es la de la sección. */
    val mapCategory: Category? = null,
    /** Si la ficha se dibuja como marcador en el mapa. */
    val onMap: Boolean = true,
    /** Página de la web con la guía completa de esta ficha (la rellena `ContentRepository`). */
    val pageId: String? = null,
    /** Fotos de la web (ruta dentro de `assets/img/`), para las fichas que no tienen foto en el diseño. */
    val thumbAsset: String? = null,
    val heroAsset: String? = null,
) {
    val isEvent: Boolean get() = category == Category.Eventos
}

enum class OfferKind { Entrada, Tour, Experiencia }

/** Entradas, tours y experiencias reservables (tabla Actividad de la base de datos de la web). */
data class Offer(
    val id: String,
    val kind: OfferKind,
    val tag: String,
    val title: String,
    val duration: String,
    val place: String,
    /** Idiomas o lo que incluye: «español/inglés», «incluye comida». */
    val extra: String,
    val priceFrom: Int,
    @param:DrawableRes val image: Int?,
    val description: String,
    val times: List<String>,
    /** Foto cuadrada para la lista de reservas (si no hay, se usa `image`). */
    @param:DrawableRes val thumb: Int? = null,
    /** Foto de la cabecera de la entrada de la reserva. */
    @param:DrawableRes val ticket: Int? = null,
    /** Ficha del lugar donde empieza la actividad, para «Cómo llegar». */
    val placeId: String? = null,
    /** Foto de la web (ruta dentro de `assets/img/`) para las ofertas que no tienen foto en el diseño. */
    val assetImage: String? = null,
)
