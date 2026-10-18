package com.cmarcillovargas.turismovalencia.data.repo

import com.cmarcillovargas.turismovalencia.R
import com.cmarcillovargas.turismovalencia.data.AppStrings
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
 * Palabras que activan una regla: [contains] se busca dentro del texto (valen las raíces: «gratuit» sirve para «gratuito» y
 * «gratuït»; un espacio al principio o al final la ciñe a palabras enteras: « ir a » no vale dentro de «voir à») y [words] solo si
 * aparecen como palabra entera (las cortas, como «art» o «sea», darían falsos positivos). Van en
 * minúsculas y sin tildes, y juntan los cuatro idiomas de la app: se entiende la pregunta en cualquiera de ellos, y la respuesta
 * sale en el idioma elegido.
 */
internal class Keywords(val contains: List<String> = emptyList(), val words: List<String> = emptyList())

/** La pregunta ya normalizada, con sus palabras separadas. */
private class Question(raw: String) {
    val text = normalizeText(raw)
    private val tokens = text.split(NonWord).filter { it.isNotEmpty() }.toSet()

    /** Con un espacio a cada lado, una expresión de [Keywords.contains] que empieza o acaba en espacio solo vale entre palabras. */
    private val padded = " $text "

    fun matches(keywords: Keywords) = keywords.contains.any { padded.contains(it) } || keywords.words.any { it in tokens }

    private companion object {
        val NonWord = Regex("[^a-z0-9·]+")
    }
}

/**
 * Lo que ValBot entiende de una pregunta, en el orden en que se prueban las reglas (gana la primera que encaja). Las palabras juntan el
 * español, el valenciano, el inglés y el francés, en ese orden dentro de cada lista.
 */
internal enum class Intent(val keywords: Keywords) {
    Hello(
        Keywords(
            contains = listOf("hola", "buenas", "buenos dias", "bon dia", "bones", "hello", "good morning", "good afternoon", "good evening", "bonjour", "bonsoir", "salut"),
            words = listOf("hi", "hey"),
        ),
    ),
    Free(
        Keywords(
            contains = listOf("gratis", "gratuit", "sin gastar", "sense gastar", "free of charge", "for free", "sans depenser"),
            words = listOf("free"),
        ),
    ),
    Route(
        Keywords(
            contains = listOf(
                "como llego", "como llegar", "como voy", " ir a ", "ruta a",
                "com arribe", "com arribar", "com vaig", "anar a ",
                "how do i get", "how to get", "how can i get", "directions", "get to ", "route to",
                "comment aller", "comment arriver", "comment se rendre", "comment j'arrive", "aller a ",
            ),
        ),
    ),
    Eat(
        Keywords(
            contains = listOf(
                "paella", "cenar", "restaurant", "arroz", "gastronom",
                "sopar", "menjar", "arros",
                "dinner", "lunch", "food", "dining", "rice",
                "manger", "diner", "dejeuner", "riz", "cuisine",
            ),
            words = listOf("comer", "comida", "comidas", "eat", "eating"),
        ),
    ),
    Christmas(
        Keywords(
            contains = listOf("navidad", "belen", "luces", "nadal", "pessebre", "llums", "christmas", "xmas", "nativity", "noel", "creche", "lumieres"),
        ),
    ),
    Itinerary(
        Keywords(
            contains = listOf(
                "3 dias", "tres dias", "itinerario", "imprescindible", "que ver",
                "3 dies", "tres dies", "itinerari", "que veure",
                "3 days", "three days", "itinerary", "must see", "must-see", "what to see", "things to see", "top sights",
                "3 jours", "trois jours", "itineraire", "incontournable", "que voir", "a voir",
            ),
        ),
    ),
    Heritage(
        Keywords(
            contains = listOf("muralla", "torres de", "serranos", "quart", "patrimonio", "unesco", "serrans", "patrimoni", "heritage", "muraille", "tours de", "patrimoine"),
            words = listOf("walls", "towers"),
        ),
    ),
    Tours(
        Keywords(
            contains = listOf(
                "visita guiada", "bus turistico", "tourist card", "tarjeta", "entrada", "ticket",
                "visites guiades", "bus turistic", "targeta", "tiquet",
                "guided", "tourist bus",
                "visite guidee", "bus touristique", "carte touristique", "billet", "entree",
            ),
            // «tour» solo entero: «tourist office» y «office de tourisme» son preguntas de contacto.
            words = listOf("tour", "tours", "pass"),
        ),
    ),
    Sports(
        Keywords(
            contains = listOf("deporte", "correr", "running", "surf", "futbol", "esport", "football", "soccer", "courir", "marathon", "maraton"),
            words = listOf("sport", "sports"),
        ),
    ),
    Art(
        Keywords(
            contains = listOf("exposicion", "exposicio", "exhibition", "gallery", "galleries", "galerie"),
            words = listOf("arte", "art", "arts", "exposition", "expositions"),
        ),
    ),
    Contact(
        Keywords(
            contains = listOf("contacto", "telefono", "email", "correo", "oficina", "contacte", "telefon", "correu", "contact", "phone", "office", "courriel", "e-mail", "telephone", "bureau"),
        ),
    ),
    Fallas(
        Keywords(
            contains = listOf("fallas", "falles", "mascleta", "pirotecn", "pyrotech", "fireworks", "feu d'artifice", "feux d'artifice"),
        ),
    ),
    Beach(
        Keywords(
            words = listOf("playa", "playas", "mar", "platja", "platges", "beach", "beaches", "sea", "plage", "plages", "mer"),
        ),
    ),
    Kids(
        Keywords(
            contains = listOf("nino", "familia", "hijos", "famille", "family", "enfant", "xiquet"),
            words = listOf("nen", "nens", "fills", "kid", "kids", "child", "children"),
        ),
    ),
    Rain(
        Keywords(
            contains = listOf("llueve", "lluvia", "museo", "museu", "museum", "musee", "pluie", "pleut"),
            words = listOf("plou", "ploure", "pluja", "rain", "rains", "rainy", "raining"),
        ),
    ),
    Lodging(
        Keywords(
            contains = listOf("hotel", "dormir", "alojamiento", "hospedar", "allotjament", "accommodation", "hostel", "lodging", "hebergement", "logement"),
            words = listOf("sleep", "stay"),
        ),
    ),
    Eco(
        Keywords(
            contains = listOf("sostenible", "bici", "sustainable", "bicycle", "durable"),
            words = listOf("eco", "ecoturismo", "ecotourism", "ecotourisme", "verde", "verd", "green", "vert", "bike", "velo"),
        ),
    ),
}

/** La intención de [question] o `null` si ninguna regla encaja. */
internal fun detectIntent(question: String): Intent? {
    val q = Question(question)
    return Intent.entries.firstOrNull { q.matches(it.keywords) }
}

/**
 * Respuestas de ValBot con reglas sobre el contenido de la app: la alternativa cuando no hay conexión o el servidor
 * (`/api/chat`, Gemini) no responde. Las respuestas en línea pasan por [decorate] para llevar sus fichas. Los textos están en
 * `strings.xml` (un archivo por idioma) y salen en el idioma de la app ([strings]).
 */
class ValBotEngine(
    private val content: ContentRepository,
    private val web: WebContentRepository,
    private val strings: AppStrings,
) : ValBotRules {

    override val greeting: String get() = strings.get(R.string.valbot_greeting)

    override val suggestions: List<String> get() = strings.array(R.array.valbot_suggestions)

    override fun reply(question: String): BotReply {
        val intent = detectIntent(question)
        return when (intent) {
            Intent.Hello -> BotReply(strings.get(R.string.valbot_reply_hello))

            Intent.Free -> {
                val free = content.all.filter { it.isFree && it.category == Category.QueVer }.sortedBy { it.distanceMeters }.take(2)
                BotReply(strings.get(R.string.valbot_reply_free), free.map { it.id })
            }

            Intent.Route -> routeReply(normalizeText(question))

            Intent.Eat -> BotReply(strings.get(R.string.valbot_reply_eat), listOf("paella", "albufera"))

            Intent.Christmas -> BotReply(strings.get(R.string.valbot_reply_christmas), pageIds = listOf("Navidad"))

            Intent.Itinerary -> BotReply(strings.get(R.string.valbot_reply_itinerary), pageIds = listOf("ValenciaTresDias", "VisitasImprescindibles"))

            Intent.Heritage -> BotReply(strings.get(R.string.valbot_reply_heritage), pageIds = listOf("CiudadAmurallada", "PatrimonioHumanidad"))

            Intent.Tours -> BotReply(strings.get(R.string.valbot_reply_tours), pageIds = listOf("ValenciaCard", "BusTuristico", "CentroHistorico"))

            Intent.Sports -> BotReply(strings.get(R.string.valbot_reply_sports), pageIds = listOf("Deportes"))

            Intent.Art -> BotReply(strings.get(R.string.valbot_reply_art), pageIds = listOf("Exposicion", "MonumentosMuseos"))

            Intent.Contact -> BotReply(strings.get(R.string.valbot_reply_contact))

            Intent.Fallas -> BotReply(strings.get(R.string.valbot_reply_fallas), listOf("fallas"))

            Intent.Beach -> BotReply(strings.get(R.string.valbot_reply_beach), listOf("playas", "la-marina"))

            Intent.Kids -> BotReply(strings.get(R.string.valbot_reply_kids), listOf("oceanografic", "jardin-turia"))

            Intent.Rain -> BotReply(strings.get(R.string.valbot_reply_rain), listOf("museo-bellas-artes", "oceanografic"))

            Intent.Lodging -> BotReply(strings.get(R.string.valbot_reply_lodging), listOf("aloj-centro", "aloj-ruzafa"))

            Intent.Eco -> BotReply(strings.get(R.string.valbot_reply_eco), listOf("jardin-turia", "albufera-barca"))

            null -> {
                val found = content.all.filter { content.matches(it, question) }.take(2)
                val guides = web.search(question).take(2).map { it.id }
                if (found.isNotEmpty() || guides.isNotEmpty()) {
                    BotReply(strings.get(R.string.valbot_reply_found), found.map { it.id }, guides)
                } else {
                    BotReply(strings.get(R.string.valbot_reply_unknown))
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

    /** [normalized] es la pregunta ya pasada por `normalizeText`. */
    private fun routeReply(normalized: String): BotReply {
        val target: ContentItem? = content.all.firstOrNull { item ->
            val title = normalizeText(item.title)
            normalized.contains(title) || item.keywords.any { it.length > 3 && normalized.contains(normalizeText(it)) }
        }
        return if (target != null) {
            BotReply(
                strings.get(R.string.valbot_reply_route, target.title, formatDistance(target.distanceMeters), walkMinutes(target.distanceMeters)),
                listOf(target.id),
            )
        } else {
            BotReply(strings.get(R.string.valbot_reply_route_ask))
        }
    }

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
