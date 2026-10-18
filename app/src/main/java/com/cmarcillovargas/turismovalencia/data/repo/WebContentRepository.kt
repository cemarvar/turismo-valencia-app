package com.cmarcillovargas.turismovalencia.data.repo

import android.content.Context
import com.cmarcillovargas.turismovalencia.data.model.Category
import com.cmarcillovargas.turismovalencia.data.model.CategoryIndex
import com.cmarcillovargas.turismovalencia.data.model.HomeTile
import com.cmarcillovargas.turismovalencia.data.model.LanguageCode
import com.cmarcillovargas.turismovalencia.data.model.WebBlock
import com.cmarcillovargas.turismovalencia.data.model.WebFamily
import com.cmarcillovargas.turismovalencia.data.model.WebFilter
import com.cmarcillovargas.turismovalencia.data.model.WebIndex
import com.cmarcillovargas.turismovalencia.data.model.WebItem
import com.cmarcillovargas.turismovalencia.data.model.WebPage
import com.cmarcillovargas.turismovalencia.data.model.WebPageMeta
import com.cmarcillovargas.turismovalencia.data.model.WebTag
import com.cmarcillovargas.turismovalencia.util.normalizeText
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Contenido de las páginas de la web (Que_Ver, Eventos, Gastronomia, Alojamientos, Entradas y la portada),
 * convertido a JSON y guardado en `assets/web/`. Las fotos van en `assets/img/` con la misma ruta que en la web.
 *
 * Solo está en español. Los títulos y las etiquetas de las páginas, las tarjetas de la portada y los títulos de las secciones salen
 * en el idioma de la app ([translations]); los textos de las páginas no (la pantalla lo avisa).
 */
// TODO(api): servir este contenido desde el backend (tabla Actividad y páginas) con caché en Room (TODO(room)).
class WebContentRepository(context: Context) {
    /** El contexto de la aplicación (lo usan quienes crean textos por idioma con él). */
    val appContext: Context = context.applicationContext
    private val assets = appContext.assets
    private val cache = ConcurrentHashMap<String, WebPage>()

    /** Traducciones del contenido propio de la app y de los títulos de la web (ver [ContentTranslations]). */
    val translations = ContentTranslations { path -> runCatching { read(path) }.getOrNull() }

    private val baseIndex: WebIndex by lazy { parseIndex(JSONObject(read("web/index.json"))) }

    @Volatile
    private var localizedIndex: Pair<LanguageCode, WebIndex>? = null

    /** Índice de la web con los títulos en el idioma de la app. */
    val index: WebIndex
        get() {
            val language = translations.language
            localizedIndex?.let { (lang, cached) -> if (lang == language) return cached }
            return translate(baseIndex).also { localizedIndex = language to it }
        }

    /** Cambia el idioma de los títulos (el contenido de las páginas sigue en español). */
    fun setLanguage(language: LanguageCode) {
        translations.setLanguage(language)
        localizedIndex = null
    }

    private fun translate(base: WebIndex): WebIndex {
        if (translations.language == LanguageCode.ES) return base
        fun tiles(list: List<HomeTile>) = list.map { tile -> translations.tile(tile.route)?.let { tile.copy(title = it) } ?: tile }
        return WebIndex(
            pages = base.pages.mapValues { (id, meta) ->
                translations.page(id)?.let { meta.copy(title = it.title ?: meta.title, eyebrow = it.eyebrow ?: meta.eyebrow) } ?: meta
            },
            esencial = tiles(base.esencial),
            planes = tiles(base.planes),
            sostenible = tiles(base.sostenible),
            categories = base.categories.mapValues { (family, section) ->
                translations.section(family.key)?.let { section.copy(title = it.title, subtitle = it.subtitle) } ?: section
            },
        )
    }

    fun meta(pageId: String): WebPageMeta? = index.pages[pageId]

    fun pagesOf(vararg families: WebFamily): List<WebPageMeta> = index.pages.values.filter { it.family in families }

    /** Página completa; se lee del JSON la primera vez y se guarda en memoria. Solo el título y la etiqueta salen traducidos. */
    suspend fun page(id: String): WebPage? {
        val page = cache[id] ?: withContext(Dispatchers.IO) {
            runCatching { parsePage(id, JSONObject(read("web/pages/$id.json"))) }.getOrNull()?.also { cache[id] = it }
        } ?: return null
        val text = translations.page(id) ?: return page
        return page.copy(title = text.title ?: page.title, eyebrow = text.eyebrow ?: page.eyebrow)
    }

    /** Página de la web que corresponde a una ficha de la app, si la hay. */
    fun pageIdFor(contentId: String): String? = ContentToPage[contentId]

    /** Foto de la web que acompaña a una oferta de Tickets & Tours, si la hay. */
    fun offerImage(offerId: String): String? = OfferToPage[offerId]?.let { meta(it) }?.let { it.hero ?: it.thumb }

    /** Índice de la sección de la web que corresponde a una categoría de la app (título, texto y fichas en el orden de la web). */
    fun categoryIndex(category: Category): CategoryIndex? = familyOf(category)?.let { index.categories[it] }

    /** Tarjeta de la web (foto y título del índice de su sección) de una página. */
    fun tileOf(category: Category, pageId: String): HomeTile? = categoryIndex(category)?.tiles?.firstOrNull { it.route == pageId }

    /** Posición de una página en el índice de su sección; las que no salen van al final. */
    fun orderOf(category: Category, pageId: String?): Int =
        categoryIndex(category)?.tiles?.indexOfFirst { it.route == pageId }?.takeIf { it >= 0 } ?: Int.MAX_VALUE

    private fun familyOf(category: Category): WebFamily? = when (category) {
        Category.QueVer -> WebFamily.QueVer
        Category.Eventos -> WebFamily.Eventos
        Category.Gastronomia -> WebFamily.Gastronomia
        Category.Alojamientos -> WebFamily.Alojamientos
        Category.Sostenible -> null
    }

    /** Ficha de la app que corresponde a una página de la web, si la hay. */
    fun contentIdFor(pageId: String): String? = PageToContent[pageId]

    /** Páginas sin ficha propia en la app (lo esencial, planes, guías de entradas y textos legales) que encajan con la búsqueda. */
    fun search(query: String): List<WebPageMeta> {
        val tokens = normalizeText(query).split(" ").filter { it.isNotBlank() }
        if (tokens.isEmpty()) return emptyList()
        return index.pages.values
            .filter { it.family in SearchableFamilies }
            .filter { meta -> val text = normalizeText(meta.title + " " + meta.eyebrow.orEmpty()); tokens.all { text.contains(it) } }
    }

    private fun read(path: String): String = assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }

    private fun parseIndex(json: JSONObject): WebIndex {
        val pages = json.getJSONObject("pages")
        val metas = pages.keys().asSequence().associateWith { id ->
            val p = pages.getJSONObject(id)
            WebPageMeta(id, WebFamily.fromKey(p.getString("family")), p.getString("title"), p.optStringOrNull("eyebrow"), p.optStringOrNull("hero"), p.optStringOrNull("thumb"))
        }
        val home = json.getJSONObject("home")
        fun tiles(key: String) = home.getJSONArray(key).objects().map { HomeTile(it.getString("image"), it.getString("title"), it.getString("route")) }
        val categories = json.optJSONObject("categories")?.let { all ->
            all.keys().asSequence().associate { key ->
                val c = all.getJSONObject(key)
                WebFamily.fromKey(key) to CategoryIndex(
                    c.getString("title"),
                    c.getString("subtitle"),
                    c.getJSONArray("tiles").objects().map { HomeTile(it.getString("image"), it.getString("title"), it.getString("route")) },
                )
            }
        }.orEmpty()
        return WebIndex(metas, tiles("esencial"), tiles("planes"), tiles("sostenible"), categories)
    }

    private fun parsePage(id: String, json: JSONObject): WebPage = WebPage(
        id = id,
        family = WebFamily.fromKey(json.getString("family")),
        title = json.getString("title"),
        eyebrow = json.optStringOrNull("eyebrow"),
        lead = json.optJSONArray("lead").strings(),
        hero = json.optStringOrNull("hero"),
        blocks = json.getJSONArray("blocks").objects().map(::parseBlock),
    )

    private fun parseBlock(b: JSONObject): WebBlock = when (b.getString("type")) {
        "facts" -> WebBlock.Facts(b.optString("title"), b.getJSONArray("rows").pairs())
        "bullets" -> WebBlock.Bullets(b.optString("title"), b.getJSONArray("items").strings())
        else -> WebBlock.Items(
            title = b.optString("title"),
            filters = b.optJSONArray("filters")?.objects()?.map { WebFilter(it.getString("id"), it.getString("label")) }.orEmpty(),
            items = b.getJSONArray("items").objects().map(::parseItem),
        )
    }

    private fun parseItem(o: JSONObject) = WebItem(
        num = o.optStringOrNull("num"),
        kicker = o.optStringOrNull("kicker"),
        title = o.optStringOrNull("title"),
        subtitle = o.optStringOrNull("subtitle"),
        body = o.optStringOrNull("body"),
        facts = o.optJSONArray("facts").pairs(),
        bullets = o.optJSONArray("bullets").strings(),
        bulletsTitle = o.optStringOrNull("bulletsTitle"),
        notes = o.optJSONArray("notes").pairs(),
        tags = o.optJSONArray("tags")?.objects()?.map { WebTag(it.getString("l"), it.optBoolean("free")) }.orEmpty(),
        image = o.optStringOrNull("image"),
        url = o.optStringOrNull("url"),
        category = o.optStringOrNull("cat"),
    )

    private fun JSONObject.optStringOrNull(key: String): String? = if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    private fun JSONArray?.strings(): List<String> = if (this == null) emptyList() else List(length()) { optString(it) }

    private fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else List(length()) { getJSONObject(it) }

    private fun JSONArray?.pairs(): List<Pair<String, String>> =
        if (this == null) emptyList() else List(length()) { val row = getJSONArray(it); row.optString(0) to row.optString(1) }

    private companion object {
        val SearchableFamilies = setOf(WebFamily.Esencial, WebFamily.Planes, WebFamily.Entradas, WebFamily.Tours, WebFamily.Experiencias, WebFamily.Legal)

        /** Ficha de la app → página de la web. */
        val ContentToPage = mapOf(
            "lonja-seda" to "LonjaSeda", "catedral-caliz" to "CatedralCaliz", "mercado-central" to "MercadoCentral", "barrio-carmen" to "BarrioCarmen",
            "oceanografic" to "Oceanografic", "albufera" to "Albufera", "iglesia-san-nicolas" to "IglesiaSanNicolas", "jardin-turia" to "JardinTuria",
            "la-marina" to "LaMarina", "mestalla" to "Mestalla", "museo-bellas-artes" to "MuseoBellasArtes", "playas" to "Playes", "ruzafa" to "Ruzafa",
            "fallas" to "Fallas", "corpus-christi" to "CorpusChristi", "gran-feria" to "GranFeria", "festivales-verano" to "FestivalesVerano", "festivos" to "Festivos",
            "paella" to "Paella", "fideua" to "Fideua", "arroz-horno" to "ArrozHorno", "arroz-negro" to "ArrozNegro", "all-i-pebre" to "AllPebre",
            "esgarraet" to "Esgarraet", "arroz-senyoret" to "ArrozSenyoret", "bunyol" to "Buenuelo", "horchata" to "Horchata",
            "aloj-centro" to "Centro", "aloj-gran-via" to "BarrioGranVia", "aloj-ruzafa" to "BarrioRuzada", "aloj-cac" to "CiudadArtesCienciasAloj",
            "aloj-playa" to "ZonaPlayaPaseoMaritimo", "aloj-barato-exclusivo" to "BaratoExclusivo",
            "ecoturismo" to "Ecoturismo", "muevete" to "MueveteValencia", "viaje-responsable" to "ViajeResponsable", "comercio-local" to "ComercioLocal",
            "consejos" to "Consejos", "viaje-sostenible" to "ViajeSostenible",
            "bioparc" to "Bioparc", "cac" to "CAC", "deportes" to "Deportes",
        )
        /** Oferta de Tickets & Tours → página de la web de la que sale su foto. */
        val OfferToPage = mapOf(
            "albufera-barca" to "Nauticas", "taller-paella" to "ActividadesGastronomicas", "kayak-albufera" to "Nauticas",
            "excursion-albufera-paella" to "Excursiones", "paddle-surf" to "Nauticas", "taller-horchata" to "ActividadesGastronomicas",
            "tour-ruzafa" to "Ruzafa", "ruta-ciutat-vella" to "CentroHistorico", "visita-catedral" to "CatedralCaliz",
            "tour-lonja-catedral" to "LonjaSeda", "tour-ciudad-artes" to "CienciasArtes", "ruta-bici" to "Bici",
            "tour-carmen-nocturno" to "BarrioCarmen", "tourist-card-72" to "ValenciaCard", "bus-turistico" to "BusTuristico",
            "entrada-oceanografic" to "Oceanografic",
        )
        val PageToContent = ContentToPage.entries.associate { (content, page) -> page to content }
    }
}
