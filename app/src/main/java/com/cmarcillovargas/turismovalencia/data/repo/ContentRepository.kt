package com.cmarcillovargas.turismovalencia.data.repo

import com.cmarcillovargas.turismovalencia.data.fake.FakeAlojamientos
import com.cmarcillovargas.turismovalencia.data.fake.FakeEventos
import com.cmarcillovargas.turismovalencia.data.fake.FakeGastronomia
import com.cmarcillovargas.turismovalencia.data.fake.FakeOfertas
import com.cmarcillovargas.turismovalencia.data.fake.FakeQueVer
import com.cmarcillovargas.turismovalencia.data.fake.FakeSostenible
import com.cmarcillovargas.turismovalencia.data.model.Category
import com.cmarcillovargas.turismovalencia.data.model.ContentItem
import com.cmarcillovargas.turismovalencia.data.model.Offer
import com.cmarcillovargas.turismovalencia.data.model.OfferKind
import com.cmarcillovargas.turismovalencia.data.model.SearchSuggestion
import com.cmarcillovargas.turismovalencia.data.model.SortMode
import com.cmarcillovargas.turismovalencia.util.formatEuro
import com.cmarcillovargas.turismovalencia.util.normalizeText

/** Contenido de solo lectura. */
// TODO(api): sustituir el contenido de ejemplo por la API (Retrofit) con caché en Room (TODO(room)).
class ContentRepository {
    val all: List<ContentItem> = FakeQueVer + FakeGastronomia + FakeAlojamientos + FakeEventos + FakeSostenible
    val offers: List<Offer> = FakeOfertas

    private val byId = all.associateBy { it.id }

    fun item(id: String): ContentItem? = byId[id]

    fun offer(id: String): Offer? = offers.firstOrNull { it.id == id }

    fun items(ids: Collection<String>): List<ContentItem> = ids.mapNotNull(byId::get)

    fun byCategory(category: Category): List<ContentItem> =
        all.filter { it.category == category && !(category == Category.Sostenible && it.subcategory == "Experiencia") }

    fun offersOf(kind: OfferKind): List<Offer> = offers.filter { it.kind == kind }

    /** Tarjetas del carrusel de destacados del inicio. */
    fun featured(): List<ContentItem> = all.filter { it.feature != null }.sortedBy { if (it.id == "fallas") 0 else 1 }

    /** Tarjetas de «Cerca de ti» en el inicio. */
    fun nearby(): List<ContentItem> = items(listOf("mercado-central", "albufera-barca", "paella", "playas"))

    fun sustainableTopics(): List<ContentItem> = all.filter { it.category == Category.Sostenible && it.subcategory == "Tema" }

    fun sorted(items: List<ContentItem>, mode: SortMode): List<ContentItem> = when (mode) {
        SortMode.Distance -> items.sortedBy { it.distanceMeters }
        SortMode.Rating -> items.sortedByDescending { it.rating ?: 0.0 }
        SortMode.Name -> items.sortedBy { normalizeText(it.title) }
    }

    fun matches(item: ContentItem, query: String): Boolean {
        val q = normalizeText(query).trim()
        if (q.isEmpty()) return false
        val haystack = (listOf(item.title, item.area, item.eyebrow, item.subcategory) + item.keywords).map(::normalizeText)
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
            suggestions += SearchSuggestion(offer.title, "Experiencia · desde ${formatEuro(offer.priceFrom)}", SearchSuggestion.Kind.Offer, offer.id)
        }
        val mapped = items.filter { it.distanceMeters >= 0 }
        if (mapped.isNotEmpty()) {
            val dish = items.firstOrNull { it.category == Category.Gastronomia }
            val title = dish?.let { "Restaurantes de ${normalizeFirstWord(it.title)} cerca" } ?: "Ver «$query» en el mapa"
            suggestions += SearchSuggestion(title, "Mapa · ${mapped.size} resultados", SearchSuggestion.Kind.Map, query)
        }
        return suggestions
    }

    private fun normalizeFirstWord(title: String): String = title.substringBefore(" ").lowercase()
}
