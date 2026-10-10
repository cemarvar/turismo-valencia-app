package com.cmarcillovargas.turismovalencia.data.repo

import com.cmarcillovargas.turismovalencia.data.model.Category
import com.cmarcillovargas.turismovalencia.data.model.ContentItem
import com.cmarcillovargas.turismovalencia.util.formatDistance
import com.cmarcillovargas.turismovalencia.util.normalizeText
import com.cmarcillovargas.turismovalencia.util.walkMinutes

data class BotReply(val text: String, val cardIds: List<String> = emptyList(), val pageIds: List<String> = emptyList())

/**
 * Respuestas de ValBot con reglas sobre el contenido de ejemplo.
 * TODO(api): el asistente real es la función chat de la web (Gemini) detrás del backend; la app nunca guarda la clave.
 */
class ValBotEngine(private val content: ContentRepository, private val web: WebContentRepository) {

    val greeting = "¡Hola! Soy ValBot, tu asistente de turismo en Valencia. ¿En qué puedo ayudarte?"

    val suggestions = listOf(
        "¿Qué ver hoy gratis en Valencia?",
        "Restaurantes de paella cerca",
        "¿Cómo llego al Jardín del Turia?",
        "¿Qué hago con niños?",
        "Planes si llueve",
        "Valencia en 3 días",
    )

    fun reply(question: String): BotReply {
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
}
