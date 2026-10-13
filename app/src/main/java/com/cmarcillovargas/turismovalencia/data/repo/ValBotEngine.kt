package com.cmarcillovargas.turismovalencia.data.repo

import com.cmarcillovargas.turismovalencia.data.model.Category
import com.cmarcillovargas.turismovalencia.data.model.ContentItem
import com.cmarcillovargas.turismovalencia.util.formatDistance
import com.cmarcillovargas.turismovalencia.util.normalizeText
import com.cmarcillovargas.turismovalencia.util.walkMinutes

data class BotReply(
    val text: String,
    val cardIds: List<String> = emptyList(),
    val pageIds: List<String> = emptyList(),
    /** Respuesta de las reglas de la app porque el asistente en línea no ha respondido. */
    val basic: Boolean = false,
)

/** Lo que ValBot necesita de las reglas de la app: saludo, sugerencias, respuesta sin conexión y fichas de una respuesta en línea. */
interface ValBotRules {
    val greeting: String
    val suggestions: List<String>

    /** Responde con reglas sobre el contenido de la app. */
    fun reply(question: String): BotReply

    /** Añade a una respuesta en línea las fichas y guías de la app que nombra. */
    fun decorate(text: String): BotReply
}

/**
 * Respuestas de ValBot con reglas sobre el contenido de la app: la alternativa cuando no hay conexión o el servidor
 * (`/api/chat`, Gemini) no responde. Las respuestas en línea pasan por [decorate] para llevar sus fichas.
 */
class ValBotEngine(private val content: ContentRepository, private val web: WebContentRepository) : ValBotRules {

    override val greeting = "¡Hola! Soy ValBot, tu asistente de turismo en Valencia. ¿En qué puedo ayudarte?"

    override val suggestions = listOf(
        "¿Qué ver hoy gratis en Valencia?",
        "Restaurantes de paella cerca",
        "¿Cómo llego al Jardín del Turia?",
        "¿Qué hago con niños?",
        "Planes si llueve",
        "Valencia en 3 días",
    )

    override fun reply(question: String): BotReply {
        val q = normalizeText(question)
        return when {
            q.hasAny("hola", "buenas", "buenos dias", "hello") ->
                BotReply("¡Hola! Pregúntame por lugares, eventos, gastronomía o cómo llegar a cualquier sitio.")

            q.hasAny("gratis", "gratuit", "sin gastar") -> {
                val free = content.all.filter { it.isFree && it.category == Category.QueVer }.sortedBy { it.distanceMeters }.take(2)
                BotReply("Hoy tienes buenas opciones sin gastar nada. Las más cercanas a ti:", free.map { it.id })
            }

            q.hasAny("como llego", "como llegar", "como voy", "ir a ", "ruta a") -> routeReply(q)

            q.hasAny("paella", "cenar", "comer", "restaurante", "arroz", "gastronom") ->
                BotReply(
                    "Para una buena paella, la cuna es La Albufera (El Palmar) y la Malvarrosa. En el centro, reserva con antelación y pide leña. Mira estas opciones:",
                    listOf("paella", "albufera"),
                )

            q.hasAny("navidad", "belen", "luces") ->
                BotReply("En Navidad, Valencia se llena de luces, mercados y belenes. Aquí tienes la guía completa:", pageIds = listOf("Navidad"))

            q.hasAny("3 dias", "tres dias", "itinerario", "imprescindible", "que ver") ->
                BotReply("Para organizarte, estas guías te sirven de base:", pageIds = listOf("ValenciaTresDias", "VisitasImprescindibles"))

            q.hasAny("muralla", "torres de", "serranos", "quart", "patrimonio", "unesco") ->
                BotReply("La Valencia monumental está en estas guías:", pageIds = listOf("CiudadAmurallada", "PatrimonioHumanidad"))

            q.hasAny("tour", "visita guiada", "bus turistico", "tourist card", "tarjeta", "entrada", "ticket") ->
                BotReply("Para tickets y tours, mira estas guías y reserva desde Tickets & Tours:", pageIds = listOf("ValenciaCard", "BusTuristico", "CentroHistorico"))

            q.hasAny("deporte", "correr", "running", "surf", "futbol") ->
                BotReply("Valencia es ciudad de deporte: carreras, bici, playa y mucho más.", pageIds = listOf("Deportes"))

            q.hasAny("exposicion", "exposiciones", "arte") ->
                BotReply("Para exposiciones y arte, empieza por aquí:", pageIds = listOf("Exposicion", "MonumentosMuseos"))

            q.hasAny("contacto", "telefono", "email", "correo", "oficina") ->
                BotReply("Puedes contactar con la oficina de turismo de lunes a viernes de 9:00 a 18:00 en el +34 963 000 000 o en info@valencia.com.")

            q.hasAny("fallas", "mascleta", "falles", "pirotecn") ->
                BotReply("Las Fallas se celebran del 1 al 19 de marzo. La mascletà es cada día a las 14:00 en la Plaza del Ayuntamiento; llega 30 minutos antes.", listOf("fallas"))

            q.hasAny("playa", "mar ") ->
                BotReply("Valencia tiene 19,5 km de arena y 8 Banderas Azules. Desde el centro, la L4 te deja en Les Arenes.", listOf("playas", "la-marina"))

            q.hasAny("nino", "familia", "hijos") ->
                BotReply("Con niños funcionan muy bien el Oceanogràfic y el Jardín del Turia (9 km sin coches, con juegos y bicis).", listOf("oceanografic", "jardin-turia"))

            q.hasAny("llueve", "lluvia", "museo") ->
                BotReply("Si llueve, apunta a los museos: el de Bellas Artes es gratuito y el Oceanogràfic tiene zonas cubiertas.", listOf("museo-bellas-artes", "oceanografic"))

            q.hasAny("hotel", "dormir", "alojamiento", "hospedar") ->
                BotReply("Para dormir, el centro histórico es lo más cómodo; Ruzafa tiene más ambiente y la zona playa, primera línea de mar.", listOf("aloj-centro", "aloj-ruzafa"))

            q.hasAny("sostenible", "eco", "bici", "verde") ->
                BotReply("Valencia fue Capital Verde Europea 2024. Un buen plan sostenible: Jardín del Turia en bici y paseo en barca eléctrica por la Albufera.", listOf("jardin-turia", "albufera-barca"))

            else -> {
                val found = content.all.filter { content.matches(it, question) }.take(2)
                val guides = web.search(question).take(2).map { it.id }
                if (found.isNotEmpty() || guides.isNotEmpty()) {
                    BotReply("Esto es lo que he encontrado:", found.map { it.id }, guides)
                } else {
                    BotReply("No tengo claro eso todavía. Prueba a preguntarme por lugares, eventos, gastronomía, alojamientos o cómo llegar a un sitio. Para dudas concretas, contacta con la oficina de turismo de Valencia.")
                }
            }
        }
    }

    override fun decorate(text: String): BotReply {
        val places = content.all
            .filter { it.category in LinkedCategories && it.id !in GenericIds }
            .map { it.id to it.title }
        val itemIds = findMentions(text, places, MaxLinks)
        val linkedPages = content.all.filter { it.id in itemIds }.mapNotNull { it.pageId }.toSet()
        val guides = web.index.pages.values.map { it.id to it.title }
        val pageIds = findMentions(text, guides, MaxLinks).filter { it !in linkedPages }.take(MaxLinks - itemIds.size)
        return BotReply(text, itemIds, pageIds)
    }

    private fun routeReply(q: String): BotReply {
        val target: ContentItem? = content.all.firstOrNull { item ->
            val title = normalizeText(item.title)
            q.contains(title) || item.keywords.any { it.length > 3 && q.contains(normalizeText(it)) }
        }
        return if (target != null) {
            BotReply(
                "${target.title} está a ${formatDistance(target.distanceMeters)}, unos ${walkMinutes(target.distanceMeters)} min a pie desde tu ubicación. Abre la ficha y pulsa «Cómo llegar» para ver la ruta:",
                listOf(target.id),
            )
        } else {
            BotReply("¿A qué lugar quieres ir? Dime el nombre y te digo cuánto se tarda andando.")
        }
    }

    private fun String.hasAny(vararg words: String) = words.any { contains(it) }

    private companion object {
        /** Alojamientos y Sostenible tienen títulos genéricos («Centro histórico», «Ecoturismo»): darían fichas fuera de lugar. */
        val LinkedCategories = setOf(Category.QueVer, Category.Gastronomia, Category.Eventos)
        val GenericIds = setOf("deportes", "festivos")
        const val MaxLinks = 2
    }
}

private val LeadingArticle = Regex("""^(?:(?:el|la|los|las|the)\s+|l['’])""")

/**
 * Ids de [names] (id → título) cuyo título aparece en [text], en el orden en que aparecen, hasta [limit].
 * Se ignora el artículo inicial («La Marina» → «marina») y los títulos de menos de 5 letras.
 */
fun findMentions(text: String, names: List<Pair<String, String>>, limit: Int): List<String> {
    val haystack = normalizeText(text)
    return names
        .mapNotNull { (id, title) ->
            val core = normalizeText(title).replace(LeadingArticle, "").trim()
            val at = if (core.length >= 5) haystack.indexOf(core) else -1
            if (at >= 0) id to at else null
        }
        .sortedBy { it.second }
        .map { it.first }
        .distinct()
        .take(limit)
}
