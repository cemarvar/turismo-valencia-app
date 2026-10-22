package com.cmarcillovargas.turismovalencia.data.repo

import android.content.Context
import com.cmarcillovargas.turismovalencia.data.model.Category
import com.cmarcillovargas.turismovalencia.data.model.CategoryIndex
import com.cmarcillovargas.turismovalencia.data.model.HomeTile
import com.cmarcillovargas.turismovalencia.data.model.LanguageCode
import com.cmarcillovargas.turismovalencia.data.model.WebFamily
import com.cmarcillovargas.turismovalencia.data.model.WebIndex
import com.cmarcillovargas.turismovalencia.data.model.WebPage
import com.cmarcillovargas.turismovalencia.data.model.WebPageMeta
import com.cmarcillovargas.turismovalencia.util.normalizeText
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Contenido de las páginas de la web (Que_Ver, Eventos, Gastronomia, Alojamientos, Entradas y la portada),
 * convertido a JSON y guardado en `assets/web/`. Las fotos van en `assets/img/` con la misma ruta que en la web.
 *
 * El original está en español. Los títulos y las etiquetas de las páginas, las tarjetas de la portada y los títulos de las secciones
 * salen en el idioma de la app ([translations]); el texto de cada página, si hay una traducción completa suya en
 * `assets/i18n/<idioma>/pages/` (ver [PageTranslation]); si no, se ve en español y la pantalla lo avisa (`WebPage.textLanguage`).
 */
// TODO(api): servir este contenido desde el backend (tabla Actividad y páginas) con caché en Room (TODO(room)).
class WebContentRepository(context: Context) {
    /** El contexto de la aplicación (lo usan quienes crean textos por idioma con él). */
    val appContext: Context = context.applicationContext
    private val assets = appContext.assets
    private val cache = ConcurrentHashMap<String, WebPage>()

    /** Las páginas ya traducidas (con su título), por idioma y página: el español no entra, que ya está en [cache]. */
    private val translatedCache = ConcurrentHashMap<String, WebPage>()

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

    /** Cambia el idioma de los títulos y de los textos de las páginas que tengan traducción. */
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

    /**
     * Página completa en el idioma de la app; se lee del JSON la primera vez y se guarda en memoria. El título y la etiqueta salen de
     * `content.json`; el texto, de la traducción de la página si existe y tiene la forma del original (si no, en español: ver
     * `WebPage.textLanguage`).
     */
    suspend fun page(id: String): WebPage? {
        val language = translations.language
        val key = "${language.name}/$id"
        translatedCache[key]?.let { return it }
        val page = cache[id] ?: withContext(Dispatchers.IO) {
            runCatching { WebPageParser.parse(id, JSONObject(read("web/pages/$id.json"))) }.getOrNull()?.also { cache[id] = it }
        } ?: return null
        val text = translations.page(id)
        val titled = if (text == null) page else page.copy(title = text.title ?: page.title, eyebrow = text.eyebrow ?: page.eyebrow)
        if (language == LanguageCode.ES) return titled
        val localized = withContext(Dispatchers.IO) {
            runCatching { PageTranslation.parse(read(PageTranslation.pathOf(language, id))) }.getOrNull()?.let { titled.translated(it, language) }
        } ?: titled
        return localized.also { translatedCache[key] = it }
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
