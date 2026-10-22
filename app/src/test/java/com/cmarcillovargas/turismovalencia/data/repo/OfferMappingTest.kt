package com.cmarcillovargas.turismovalencia.data.repo

import com.cmarcillovargas.turismovalencia.data.fake.FakeOfertas
import com.cmarcillovargas.turismovalencia.data.model.LanguageCode
import com.cmarcillovargas.turismovalencia.data.model.OfferKind
import com.cmarcillovargas.turismovalencia.data.remote.OfferDto
import com.cmarcillovargas.turismovalencia.data.remote.OfferTranslationDto
import com.cmarcillovargas.turismovalencia.data.remote.OffersResponse
import com.cmarcillovargas.turismovalencia.data.remote.TurismoApiClient
import com.cmarcillovargas.turismovalencia.data.remote.contractExample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class OfferMappingTest {
    private val catalog: List<OfferDto> = TurismoApiClient.Json.decodeFromString<OffersResponse>(contractExample("offers-list.json")).offers

    private fun dto(
        id: String = "oferta",
        kind: String = "tour",
        title: String = "Una oferta",
        cents: Int = 1500,
        placeId: String? = null,
        slots: List<String> = listOf("10:00"),
    ) = OfferDto(id = id, kind = kind, title = title, priceFromCents = cents, placeId = placeId, slots = slots)

    @Test
    fun `las ofertas del contrato son validas para la app`() {
        val entities = catalog.mapIndexed { index, offer -> offer.toEntity(index) }

        assertTrue("alguna oferta del contrato no se puede guardar", entities.all { it != null })
        assertEquals(catalog.map { it.id }, entities.map { it!!.id })
        assertEquals(catalog.indices.toList(), entities.map { it!!.position })
        assertEquals(listOf(OfferKind.Experiencia, OfferKind.Entrada, OfferKind.Entrada), entities.map { it!!.kind })
    }

    @Test
    fun `las ofertas del contrato estan en el catalogo incluido en la app con los mismos datos`() {
        // El respaldo sin conexión y la presentación (fotos por id) solo valen si hablan de las mismas ofertas que el servidor.
        val bundledById = FakeOfertas.associateBy { it.id }
        catalog.forEach { server ->
            val bundled = bundledById[server.id] ?: error("El catálogo incluido en la app no tiene «${server.id}», que sí está en el contrato")
            assertEquals(server.id, bundled.kind, offerKindOf(server.kind))
            assertEquals(server.id, bundled.title, server.title)
            assertEquals(server.id, bundled.priceFromCents.toLong(), server.priceFromCents.toLong())
            assertEquals(server.id, bundled.times, server.slots)
            assertEquals(server.id, bundled.placeId, server.placeId)
        }
    }

    @Test
    fun `las traducciones del contrato se guardan y se leen por idioma`() {
        val albufera = catalog.first { it.id == "albufera-barca" }.toEntity(0)!!.toModel(bundled = null, assetImage = null)

        assertEquals(setOf(LanguageCode.VA, LanguageCode.EN, LanguageCode.FR), albufera.serverTexts.keys)
        val english = albufera.serverTexts.getValue(LanguageCode.EN)
        assertEquals("Boat trip on La Albufera", english.title)
        assertEquals("Excursion", english.tag)
        // El original sigue siendo el español del servidor: lo que cambia el idioma es la lectura, no lo guardado.
        assertEquals("Paseo en barca por la Albufera", albufera.title)
        assertEquals("Excursión", albufera.tagKey)
    }

    @Test
    fun `las traducciones del contrato son las mismas que la app trae incluidas`() {
        // El servidor y `assets/i18n` dicen lo mismo de las ofertas del contrato (y el backend lo comprueba contra el contenido de la app).
        listOf(LanguageCode.VA, LanguageCode.EN, LanguageCode.FR).forEach { language ->
            val bundled = ContentTranslations.parse(File("src/main/assets/${ContentTranslations.pathOf(language)}").readText(Charsets.UTF_8))
            catalog.forEach { server ->
                val fromServer = server.translations.getValue(language.name.lowercase())
                val fromApp = bundled.offers.getValue(server.id)
                val where = "$language/${server.id}"
                assertEquals(where, fromApp.title, fromServer.title)
                assertEquals(where, fromApp.tag, fromServer.tag)
                assertEquals(where, fromApp.duration, fromServer.duration)
                assertEquals(where, fromApp.place, fromServer.place)
                assertEquals(where, fromApp.extra, fromServer.extra)
                assertEquals(where, fromApp.description, fromServer.description)
            }
        }
    }

    @Test
    fun `un idioma que la app no tiene o una traduccion vacia no se guardan`() {
        val offer = dto().copy(
            translations = mapOf(
                "de" to OfferTranslationDto(title = "Angebot"), // la app no está en alemán
                "en" to OfferTranslationDto(), // sin ningún texto
                "fr" to OfferTranslationDto(title = " ", description = "Texte"), // un texto en blanco es como no tenerlo
                "es" to OfferTranslationDto(title = "Oferta"), // el español es el original, no una traducción
            ),
        )

        val texts = offer.toEntity(0)!!.toModel(bundled = null, assetImage = null).serverTexts

        assertEquals(setOf(LanguageCode.FR), texts.keys)
        assertNull(texts.getValue(LanguageCode.FR).title)
        assertEquals("Texte", texts.getValue(LanguageCode.FR).description)
    }

    @Test
    fun `unas traducciones guardadas que no se pueden leer no rompen la app`() {
        assertEquals(emptyMap<LanguageCode, Any>(), decodeTranslations("esto no es JSON"))
        assertEquals(emptyMap<LanguageCode, Any>(), decodeTranslations("{}"))
        assertEquals(setOf(LanguageCode.EN), decodeTranslations("""{"en":{"title":"Hi"},"xx":{"title":"?"}}""").keys)
    }

    @Test
    fun `el catalogo incluido en la app es coherente`() {
        assertEquals("ids repetidos", FakeOfertas.size, FakeOfertas.map { it.id }.toSet().size)
        FakeOfertas.forEach { offer ->
            assertTrue("«${offer.id}» no tiene horarios", offer.times.isNotEmpty())
            assertTrue("«${offer.id}» no tiene precio", offer.priceFromCents > 0)
            assertTrue("«${offer.id}» no tiene título", offer.title.isNotBlank())
        }
    }

    @Test
    fun `el tipo de la API se traduce y uno desconocido no se acepta`() {
        assertEquals(OfferKind.Entrada, offerKindOf("entrada"))
        assertEquals(OfferKind.Tour, offerKindOf(" Tour "))
        assertEquals(OfferKind.Experiencia, offerKindOf("EXPERIENCIA"))
        assertNull(offerKindOf("abono"))
        assertNull(dto(kind = "abono").toEntity(0))
    }

    @Test
    fun `una oferta sin id, sin titulo o con precio negativo no se guarda`() {
        assertNull(dto(id = " ").toEntity(0))
        assertNull(dto(title = "").toEntity(0))
        assertNull(dto(cents = -1).toEntity(0))
        assertNotNull(dto(cents = 0).toEntity(0))
    }

    @Test
    fun `el lugar vacio es que no hay lugar y los horarios sobreviven a cualquier texto`() {
        assertNull(dto(placeId = "").toEntity(0)!!.placeId)
        assertEquals("mercado-central", dto(placeId = "mercado-central").toEntity(0)!!.placeId)

        val tricky = listOf("10:00", "Cualquier hora", "a \"b\"", "línea\ndos", "ñandú · ✓")
        assertEquals(tricky, decodeSlots(dto(slots = tricky).toEntity(0)!!.slots))
        assertEquals(emptyList<String>(), decodeSlots(dto(slots = emptyList()).toEntity(0)!!.slots))
    }

    @Test
    fun `una cache ilegible no cierra la app`() {
        assertEquals(emptyList<String>(), decodeSlots("esto no es JSON"))
        assertEquals(emptyList<String>(), decodeSlots("{\"a\":1}"))
        assertEquals(emptyList<String>(), decodeSlots(""))
    }

    @Test
    fun `la oferta lleva los datos del servidor y las fotos del catalogo incluido`() {
        val bundled = FakeOfertas.first { it.id == "albufera-barca" }
        // El servidor cambió el precio y los horarios: manda el servidor. Las fotos del diseño se conservan.
        val entity = dto(id = "albufera-barca", kind = "experiencia", title = "Paseo en barca", cents = 2050, placeId = "albufera-barca", slots = listOf("09:00", "10:00"))
            .toEntity(3)!!

        val offer = entity.toModel(bundled, assetImage = "img/Albufera/hero.jpg")

        assertEquals("Paseo en barca", offer.title)
        assertEquals(2050, offer.priceFromCents)
        assertEquals(listOf("09:00", "10:00"), offer.times)
        assertEquals(OfferKind.Experiencia, offer.kind)
        assertEquals(bundled.image, offer.image)
        assertEquals(bundled.thumb, offer.thumb)
        assertEquals(bundled.ticket, offer.ticket)
        assertNotNull("la reserva necesita la foto de la entrada", offer.ticket)
        assertEquals("img/Albufera/hero.jpg", offer.assetImage)
    }

    @Test
    fun `una oferta nueva del servidor se pinta sin fotos del diseno`() {
        val offer = dto(id = "oferta-nueva").toEntity(0)!!.toModel(bundled = null, assetImage = null)

        assertEquals("oferta-nueva", offer.id)
        assertNull(offer.image)
        assertNull(offer.thumb)
        assertNull(offer.ticket)
        assertNull(offer.assetImage)
    }
}
