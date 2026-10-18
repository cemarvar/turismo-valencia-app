package com.cmarcillovargas.turismovalencia.data.repo

import com.cmarcillovargas.turismovalencia.R
import com.cmarcillovargas.turismovalencia.data.AppStrings
import com.cmarcillovargas.turismovalencia.data.fake.FakeAlojamientos
import com.cmarcillovargas.turismovalencia.data.fake.FakeEventos
import com.cmarcillovargas.turismovalencia.data.fake.FakeEventosWeb
import com.cmarcillovargas.turismovalencia.data.fake.FakeGastronomia
import com.cmarcillovargas.turismovalencia.data.fake.FakeOfertas
import com.cmarcillovargas.turismovalencia.data.fake.FakeQueVer
import com.cmarcillovargas.turismovalencia.data.fake.FakeQueVerWeb
import com.cmarcillovargas.turismovalencia.data.fake.FakeSostenible
import com.cmarcillovargas.turismovalencia.data.model.Category
import com.cmarcillovargas.turismovalencia.data.model.ContentItem
import com.cmarcillovargas.turismovalencia.data.model.LanguageCode
import com.cmarcillovargas.turismovalencia.data.model.LatLon
import com.cmarcillovargas.turismovalencia.data.model.Offer
import com.cmarcillovargas.turismovalencia.data.model.OfferKind
import com.cmarcillovargas.turismovalencia.data.model.SearchSuggestion
import com.cmarcillovargas.turismovalencia.data.model.SortMode
import com.cmarcillovargas.turismovalencia.data.model.WebFamily
import com.cmarcillovargas.turismovalencia.util.ValenciaCenter
import com.cmarcillovargas.turismovalencia.util.distanceMeters as metersBetween
import com.cmarcillovargas.turismovalencia.util.formatPrice
import com.cmarcillovargas.turismovalencia.util.normalizeText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt

/**
 * Contenido de solo lectura. Las distancias de las fichas se miden desde [origin] (la ubicación del usuario o el centro de
 * Valencia) con las coordenadas reales de cada lugar; quien las muestre debe volver a leer cuando [origin] cambie. Los textos salen
 * en el idioma de la app ([setLanguage]; ver `ContentTranslations`): quien los muestre debe volver a leer cuando cambie el idioma, y
 * `MainActivity` lo asegura recreando las pantallas.
 */
// TODO(api): el resto del contenido (fichas y guías) sigue siendo el de la app; el catálogo de ofertas ya viene del servidor.
class ContentRepository(
    private val web: WebContentRepository,
    /** Catálogo de ofertas vigente (`OffersRepository.offers`); sin él, el incluido en la app. */
    offersSource: StateFlow<List<Offer>> = MutableStateFlow(FakeOfertas.map { it.copy(assetImage = web.offerImage(it.id)) }),
    private val strings: AppStrings = AppStrings(web.appContext),
) {
    private val translations get() = web.translations

    /** Fichas con el orden, los títulos y las fotos de los índices de la web; las del diseño del Figma conservan su título y su foto. */
    private fun buildBase(): List<ContentItem> = (FakeQueVer + FakeQueVerWeb + FakeGastronomia + FakeAlojamientos + FakeEventos + FakeEventosWeb + FakeSostenible).map { item ->
        val pageId = web.pageIdFor(item.id)
        val meta = pageId?.let(web::meta)
        val tile = pageId?.let { web.tileOf(item.category, it) }
        val fromDesign = item.id in DesignTitles
        val text = translations.item(item.id)
        val localized = if (text != null) item.translated(text) else item
        localized.copy(
            // Con traducción manda ella; sin ella, el título del Figma o el del índice de la web (en español).
            title = if (text?.title != null || fromDesign || tile == null) localized.title else tile.title,
            detailTitle = localized.detailTitle ?: meta?.title?.takeIf { !fromDesign && tile != null },
            pageId = pageId,
            thumbAsset = tile?.image ?: meta?.thumb,
            heroAsset = meta?.hero,
        )
    }

    private var base: List<ContentItem> = buildBase()

    /**
     * Los textos de cada ficha por los que se busca, ya normalizados (título, zona, etiqueta, subcategoría y palabras clave). Se
     * calculan una vez por idioma y no en cada letra que se escribe: el buscador filtra todas las fichas con cada pulsación.
     */
    @Volatile
    private var haystacks: Map<String, List<String>> = haystacksOf(base)

    private fun haystacksOf(items: List<ContentItem>): Map<String, List<String>> =
        items.associate { item -> item.id to (listOf(item.title, item.area, item.eyebrow, item.subcategory) + item.keywords).map(::normalizeText) }

    /** Entradas, tours y experiencias: las del servidor o, si no ha llegado ninguna versión, las de la app. Cambia cuando llega una nueva. */
    val offersFlow: Flow<List<Offer>> = offersSource.map(::localize)
    private val offersRaw = offersSource
    val offers: List<Offer> get() = localize(offersRaw.value)

    @Volatile
    private var localizedOffers: Triple<List<Offer>, LanguageCode, List<Offer>>? = null

    /** Las ofertas con los textos del idioma de la app (las que no tienen traducción, como una oferta nueva del servidor, se ven tal cual). */
    private fun localize(source: List<Offer>): List<Offer> {
        val language = translations.language
        if (language == LanguageCode.ES) return source
        localizedOffers?.let { (from, lang, result) -> if (from === source && lang == language) return result }
        return source.map { offer -> translations.offer(offer.id)?.let(offer::translated) ?: offer }.also { localizedOffers = Triple(source, language, it) }
    }

    /** Cambia el idioma de los textos del contenido y de la web. */
    fun setLanguage(language: LanguageCode) {
        web.setLanguage(language)
        base = buildBase()
        haystacks = haystacksOf(base)
        snapshot = snapshotFor(_origin.value)
    }

    private class Snapshot(val items: List<ContentItem>) {
        val byId = items.associateBy { it.id }
    }

    private val _origin = MutableStateFlow(ValenciaCenter)

    /** Punto desde el que se miden las distancias. */
    val origin: StateFlow<LatLon> = _origin.asStateFlow()

    @Volatile
    private var snapshot = snapshotFor(ValenciaCenter)

    /** Todas las fichas, con la distancia desde [origin]. */
    val all: List<ContentItem> get() = snapshot.items

    /** Cambia el punto de partida y recalcula las distancias. */
    fun updateOrigin(point: LatLon) {
        if (point == _origin.value) return
        snapshot = snapshotFor(point)
        _origin.value = point
    }

    private fun snapshotFor(point: LatLon) = Snapshot(base.map { it.measuredFrom(point) })

    /**
     * Las fichas con distancia en el diseño la calculan desde sus coordenadas; las que no la llevan (platos, eventos, temas)
     * siguen sin ella, y `0` significa «sin distancia».
     */
    private fun ContentItem.measuredFrom(point: LatLon): ContentItem {
        val place = location ?: return this
        if (distanceMeters <= 0) return this
        return copy(distanceMeters = metersBetween(point, place).roundToInt().coerceAtLeast(1))
    }

    fun item(id: String): ContentItem? = snapshot.byId[id]

    fun offer(id: String): Offer? = offers.firstOrNull { it.id == id }

    fun items(ids: Collection<String>): List<ContentItem> = ids.mapNotNull { snapshot.byId[it] }

    /** Fichas de una sección en el orden de su índice en la web. */
    fun byCategory(category: Category): List<ContentItem> =
        all.filter { it.category == category && !(category == Category.Sostenible && it.subcategory == "Experiencia") }
            .sortedBy { web.orderOf(category, it.pageId) }

    /** Título y texto del índice de la sección en la web («Tradición culinaria»…), si lo tiene. */
    fun sectionIndex(category: Category) = web.categoryIndex(category)

    fun offersOf(kind: OfferKind): List<Offer> = offers.filter { it.kind == kind }

    /** Tarjetas del carrusel de destacados del inicio. */
    fun featured(): List<ContentItem> = all.filter { it.feature != null }.sortedBy { if (it.id == "fallas") 0 else 1 }

    /** Carrusel del inicio de una sección: los destacados del diseño o las primeras fichas de la sección. */
    fun featuredFor(category: Category?): List<ContentItem> = when (category) {
        null, Category.QueVer -> featured()
        else -> byCategory(category).take(FeaturedPerSection)
    }

    /** Tarjetas de «Cerca de ti» en el inicio. */
    fun nearby(): List<ContentItem> = items(listOf("mercado-central", "albufera-barca", "paella", "playas"))

    fun sustainableTopics(): List<ContentItem> = all.filter { it.category == Category.Sostenible && it.subcategory == "Tema" }

    fun sorted(items: List<ContentItem>, mode: SortMode): List<ContentItem> = when (mode) {
        // Solo «Qué ver» se ordena por cercanía; el resto de secciones conserva el orden de su índice en la web.
        SortMode.Distance -> if (items.all { it.category == Category.QueVer }) items.sortedBy { it.distanceMeters } else items
        SortMode.Rating -> items.sortedByDescending { it.rating ?: 0.0 }
        SortMode.Name -> items.sortedBy { normalizeText(it.title) }
    }

    fun matches(item: ContentItem, query: String): Boolean {
        val q = normalizeText(query).trim()
        if (q.isEmpty()) return false
        val haystack = haystacks[item.id] ?: (listOf(item.title, item.area, item.eyebrow, item.subcategory) + item.keywords).map(::normalizeText)
        return q.split(" ").filter { it.isNotBlank() }.all { token -> haystack.any { it.contains(token) } }
    }

    /** Sugerencias del buscador: fichas, ofertas y una entrada para verlo en el mapa. */
    fun search(query: String): List<SearchSuggestion> {
        val q = normalizeText(query).trim()
        if (q.isEmpty()) return emptyList()
        val items = all.filter { matches(it, q) }
        val offers = offers.filter { offer ->
            q.split(" ").filter { it.isNotBlank() }.all { token -> normalizeText(offer.title + " " + offer.place + " " + offer.tag).contains(token) }
        }
        val suggestions = mutableListOf<SearchSuggestion>()
        items.take(2).forEach { item ->
            val kind = if (item.category == Category.Gastronomia) SearchSuggestion.Kind.Dish else SearchSuggestion.Kind.Place
            suggestions += SearchSuggestion(item.title, item.area.takeIf { item.distanceMeters <= 0 } ?: "${item.eyebrow} · ${item.area}", kind, item.id)
        }
        offers.take(1).forEach { offer ->
            suggestions += SearchSuggestion(offer.title, strings.get(R.string.search_offer_subtitle, formatPrice(offer.priceFromCents)), SearchSuggestion.Kind.Offer, offer.id)
        }
        web.search(q).take(2).forEach { page ->
            suggestions += SearchSuggestion(page.title, page.family.label(), SearchSuggestion.Kind.Guide, page.id)
        }
        val mapped = items.filter { it.distanceMeters >= 0 }
        if (mapped.isNotEmpty()) {
            val dish = items.firstOrNull { it.category == Category.Gastronomia }
            val title = dish?.let { strings.get(R.string.search_map_dish, normalizeFirstWord(it.title)) } ?: strings.get(R.string.search_map_query, query)
            suggestions += SearchSuggestion(title, strings.plural(R.plurals.search_map_count, mapped.size, mapped.size), SearchSuggestion.Kind.Map, query)
        }
        return suggestions
    }

    private companion object {
        const val FeaturedPerSection = 5

        /** Fichas que salen en las pantallas del Figma: conservan su título y su foto del diseño. */
        val DesignTitles = setOf("lonja-seda", "catedral-caliz", "mercado-central", "barrio-carmen", "oceanografic", "albufera", "fallas", "paella")
    }

    private fun normalizeFirstWord(title: String): String = title.substringBefore(" ").lowercase()

    private fun WebFamily.label(): String = strings.get(
        when (this) {
            WebFamily.Esencial -> R.string.search_family_esencial
            WebFamily.Planes -> R.string.search_family_planes
            WebFamily.Entradas, WebFamily.Tours, WebFamily.Experiencias -> R.string.cat_tickets
            WebFamily.Legal -> R.string.search_family_legal
            else -> R.string.search_family_guide
        },
    )
}
