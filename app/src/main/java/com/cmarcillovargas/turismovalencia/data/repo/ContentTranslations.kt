package com.cmarcillovargas.turismovalencia.data.repo

import com.cmarcillovargas.turismovalencia.data.model.Act
import com.cmarcillovargas.turismovalencia.data.model.ContentItem
import com.cmarcillovargas.turismovalencia.data.model.InfoRow
import com.cmarcillovargas.turismovalencia.data.model.LanguageCode
import com.cmarcillovargas.turismovalencia.data.model.Offer
import com.cmarcillovargas.turismovalencia.data.model.Stat
import org.json.JSONArray
import org.json.JSONObject

/**
 * Traducciones del contenido propio de la app (fichas, ofertas, títulos de las páginas de la web y de sus secciones) a valenciano,
 * inglés y francés. El español es el contenido original (`data/fake`, `assets/web`): no tiene archivo. Cada idioma vive en
 * `assets/i18n/<va|en|fr>/content.json` y solo trae lo que sustituye al español; lo que falta se ve en español.
 *
 * Los textos largos de las páginas de la web (las guías) **no están traducidos**: solo sus títulos. La pantalla avisa de ello.
 * Los ids, las categorías, las coordenadas, las fotos y las claves que usa el código (por ejemplo, el tipo de oferta) no se tocan.
 */
class ContentTranslations(private val read: (path: String) -> String?) {
    @Volatile
    private var texts: LanguageTexts = LanguageTexts.Empty

    @Volatile
    var language: LanguageCode = LanguageCode.ES
        private set

    /** Cambia el idioma y carga su archivo (el español no tiene: se ve el contenido original). */
    fun setLanguage(value: LanguageCode) {
        if (value == language && (value == LanguageCode.ES || texts !== LanguageTexts.Empty)) return
        texts = if (value == LanguageCode.ES) LanguageTexts.Empty else read(pathOf(value))?.let(::parse) ?: LanguageTexts.Empty
        language = value
    }

    fun item(id: String): ItemText? = texts.items[id]

    fun offer(id: String): OfferText? = texts.offers[id]

    fun page(id: String): PageText? = texts.pages[id]

    /** Título de la tarjeta de la portada de una página («Lo esencial», «Planes», «Turismo sostenible»), si difiere del de la página. */
    fun tile(route: String): String? = texts.tiles[route]

    fun section(key: String): SectionText? = texts.sections[key]

    companion object {
        fun pathOf(language: LanguageCode) = "i18n/${language.name.lowercase()}/content.json"

        fun parse(json: String): LanguageTexts {
            val root = JSONObject(json)
            return LanguageTexts(
                items = root.optJSONObject("items").entriesOf { key, o -> key to itemText(o) },
                offers = root.optJSONObject("offers").entriesOf { key, o -> key to offerText(o) },
                pages = root.optJSONObject("pages").entriesOf { key, o -> key to PageText(o.optStringOrNull("title"), o.optStringOrNull("eyebrow")) },
                tiles = root.optJSONObject("tiles")?.let { o -> o.keys().asSequence().associateWith { o.getString(it) } }.orEmpty(),
                sections = root.optJSONObject("sections").entriesOf { key, o -> key to SectionText(o.getString("title"), o.getString("subtitle")) },
            )
        }

        private fun itemText(o: JSONObject) = ItemText(
            subcategory = o.optStringOrNull("subcategory"),
            eyebrow = o.optStringOrNull("eyebrow"),
            title = o.optStringOrNull("title"),
            detailTitle = o.optStringOrNull("detailTitle"),
            detailEyebrow = o.optStringOrNull("detailEyebrow"),
            area = o.optStringOrNull("area"),
            ratingCount = o.optStringOrNull("ratingCount"),
            summary = o.optStringOrNull("summary"),
            description = o.optStringOrNull("description"),
            stats = o.optJSONArray("stats").pairs(),
            info = o.optJSONArray("info").pairs(),
            acts = o.optJSONArray("acts").pairs(),
            dateLabel = o.optStringOrNull("dateLabel"),
            openUntil = o.optStringOrNull("openUntil"),
            featureLabel = o.optStringOrNull("featureLabel"),
            featureSubtitle = o.optStringOrNull("featureSubtitle"),
            keywords = o.optJSONArray("keywords").strings(),
        )

        private fun offerText(o: JSONObject) = OfferText(
            tag = o.optStringOrNull("tag"),
            title = o.optStringOrNull("title"),
            duration = o.optStringOrNull("duration"),
            place = o.optStringOrNull("place"),
            extra = o.optStringOrNull("extra"),
            description = o.optStringOrNull("description"),
        )

        private fun <T> JSONObject?.entriesOf(read: (String, JSONObject) -> Pair<String, T>): Map<String, T> =
            if (this == null) emptyMap() else keys().asSequence().associate { key -> read(key, getJSONObject(key)) }

        private fun JSONObject.optStringOrNull(key: String): String? = if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

        private fun JSONArray?.strings(): List<String> = if (this == null) emptyList() else List(length()) { optString(it) }

        private fun JSONArray?.pairs(): List<Pair<String, String>> =
            if (this == null) emptyList() else List(length()) { val row = getJSONArray(it); row.optString(0) to row.optString(1) }
    }
}

/** Lo traducido de un idioma. */
class LanguageTexts(
    val items: Map<String, ItemText>,
    val offers: Map<String, OfferText>,
    val pages: Map<String, PageText>,
    val tiles: Map<String, String>,
    val sections: Map<String, SectionText>,
) {
    companion object {
        val Empty = LanguageTexts(emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap())
    }
}

/**
 * Textos traducidos de una ficha. `stats`, `info` y `acts` van en el mismo orden que los originales (el tipo de cada fila de `info` y
 * las fotos de `acts` no cambian); `keywords` se suman a las originales para que se pueda buscar en los dos idiomas.
 */
class ItemText(
    val subcategory: String? = null,
    val eyebrow: String? = null,
    val title: String? = null,
    val detailTitle: String? = null,
    val detailEyebrow: String? = null,
    val area: String? = null,
    val ratingCount: String? = null,
    val summary: String? = null,
    val description: String? = null,
    val stats: List<Pair<String, String>> = emptyList(),
    val info: List<Pair<String, String>> = emptyList(),
    val acts: List<Pair<String, String>> = emptyList(),
    val dateLabel: String? = null,
    val openUntil: String? = null,
    val featureLabel: String? = null,
    val featureSubtitle: String? = null,
    val keywords: List<String> = emptyList(),
)

class OfferText(
    val tag: String? = null,
    val title: String? = null,
    val duration: String? = null,
    val place: String? = null,
    val extra: String? = null,
    val description: String? = null,
)

class PageText(val title: String? = null, val eyebrow: String? = null)

class SectionText(val title: String, val subtitle: String)

/**
 * Ficha con los textos de [t] en lugar de los originales. La subcategoría no se traduce en las fichas cuyo valor usa el código como
 * clave («Tema», «Experiencia»: ver `ContentRepository`).
 */
internal fun ContentItem.translated(t: ItemText): ContentItem = copy(
    subcategory = if (subcategory in CodeKeys) subcategory else t.subcategory ?: subcategory,
    eyebrow = t.eyebrow ?: eyebrow,
    title = t.title ?: title,
    detailTitle = t.detailTitle ?: detailTitle,
    detailEyebrow = t.detailEyebrow ?: detailEyebrow,
    area = t.area ?: area,
    ratingCount = t.ratingCount ?: ratingCount,
    summary = t.summary ?: summary,
    description = t.description ?: description,
    stats = stats.mapIndexed { i, s -> t.stats.getOrNull(i)?.let { Stat(it.first, it.second) } ?: s },
    info = info.mapIndexed { i, r -> t.info.getOrNull(i)?.let { InfoRow(r.kind, it.first, it.second) } ?: r },
    acts = acts.mapIndexed { i, a -> t.acts.getOrNull(i)?.let { Act(it.first, it.second, a.image) } ?: a },
    dateLabel = t.dateLabel ?: dateLabel,
    openUntil = t.openUntil ?: openUntil,
    featureLabel = t.featureLabel ?: featureLabel,
    featureSubtitle = t.featureSubtitle ?: featureSubtitle,
    keywords = keywords + t.keywords,
)

internal fun Offer.translated(t: OfferText): Offer = copy(
    tag = t.tag ?: tag,
    title = t.title ?: title,
    duration = t.duration ?: duration,
    place = t.place ?: place,
    extra = t.extra ?: extra,
    description = t.description ?: description,
)

/** Valores de `ContentItem.subcategory` que el código compara con el español (no son textos para el usuario). */
private val CodeKeys = setOf("Tema", "Experiencia")
