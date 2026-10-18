package com.cmarcillovargas.turismovalencia.data.repo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** ValBot entiende la pregunta en cualquiera de los cuatro idiomas de la app, sea cual sea el idioma elegido. */
class ValBotIntentTest {
    private fun assertIntent(expected: Intent?, vararg questions: String) {
        questions.forEach { assertEquals("«$it»", expected, detectIntent(it)) }
    }

    @Test
    fun `saludos`() {
        assertIntent(Intent.Hello, "Hola", "Buenos días", "Bon dia!", "Hello there", "hi", "Hey!", "Bonjour", "Salut ValBot")
    }

    @Test
    fun `gratis`() {
        assertIntent(Intent.Free, "¿Qué ver hoy gratis en Valencia?", "Coses gratuïtes", "Is there anything free?", "Quelque chose de gratuit ?")
    }

    @Test
    fun `como llegar`() {
        assertIntent(Intent.Route, "¿Cómo llego al Jardín del Turia?", "Com arribe a la Llotja?", "How do I get to the beach?", "Comment aller à la Lonja ?")
    }

    @Test
    fun `comer`() {
        assertIntent(Intent.Eat, "Restaurantes de paella cerca", "On puc menjar paella?", "Where can I eat paella?", "Où manger une bonne paella ?", "Best rice dishes")
    }

    @Test
    fun `navidad y fallas`() {
        assertIntent(Intent.Christmas, "Planes de Navidad", "Què fer per Nadal", "Christmas lights", "Noël à Valence")
        assertIntent(Intent.Fallas, "¿Cuándo es la mascletà?", "Quan són les Falles?", "When are the fireworks?", "Les Fallas, c’est quand ?")
    }

    @Test
    fun `itinerario`() {
        assertIntent(Intent.Itinerary, "Valencia en 3 días", "València en 3 dies", "Valencia in 3 days", "Valence en 3 jours", "What to see in Valencia", "Que voir à Valence ?")
    }

    @Test
    fun `entradas y tours`() {
        assertIntent(Intent.Tours, "Entradas para el Oceanogràfic", "Una visita guiada", "Guided tour tickets", "Un tour por el centro", "Billets pour le bus touristique")
    }

    @Test
    fun `deporte, arte, contacto y playa`() {
        assertIntent(Intent.Sports, "Quiero correr", "Vull fer esport", "Where can I go running?", "Le marathon")
        assertIntent(Intent.Art, "Exposiciones de arte", "Exposicions", "Art exhibitions", "Quelles expositions ?")
        assertIntent(Intent.Contact, "Teléfono de la oficina", "Telèfon de l’oficina", "Tourist office phone", "Courriel de l’office de tourisme")
        assertIntent(Intent.Beach, "La playa", "La platja", "Beach near the centre", "La plage")
    }

    @Test
    fun `ninos, lluvia, hoteles y sostenible`() {
        assertIntent(Intent.Kids, "¿Qué hago con niños?", "Què faig amb xiquets?", "Things to do with kids", "Activités pour enfants")
        assertIntent(Intent.Rain, "Planes si llueve", "Plans si plou", "What to do if it rains", "Que faire quand il pleut")
        assertIntent(Intent.Lodging, "Dónde dormir", "On dormir", "Where to stay", "Hébergement pas cher")
        assertIntent(Intent.Eco, "Plan sostenible en bici", "Pla sostenible", "Sustainable plans", "Un plan durable")
    }

    @Test
    fun `las palabras cortas solo valen enteras`() {
        // «art» no puede activarse con «start», «sea» con «search», «hi» con «this»…
        assertNull(detectIntent("start the search for this"))
        assertEquals(null, detectIntent("research"))
        // «rain» no debe saltar en «training» ni «eco» en «second».
        assertNull(detectIntent("training second"))
    }

    @Test
    fun `una pregunta sin reglas no tiene intencion`() {
        assertNull(detectIntent("blablabla"))
        assertNull(detectIntent(""))
    }

    @Test
    fun `gana la primera regla que encaja`() {
        // «gratis» va antes que «que ver»: la respuesta es la de las opciones gratuitas.
        assertEquals(Intent.Free, detectIntent("¿Qué ver hoy gratis?"))
    }
}
