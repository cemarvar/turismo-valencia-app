package com.cmarcillovargas.turismovalencia.data.remote

import com.cmarcillovargas.turismovalencia.data.model.LatLon
import com.cmarcillovargas.turismovalencia.data.model.RouteProfile
import com.cmarcillovargas.turismovalencia.data.model.RouteStep
import com.cmarcillovargas.turismovalencia.data.model.StepKind
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/** Respuesta recortada de `routing.openstreetmap.de/routed-foot` de la Plaza del Ayuntamiento al Mercado Central. */
private const val FootJson = """
{"code":"Ok","routes":[{"geometry":{"type":"LineString","coordinates":[[-0.3763,39.4699],[-0.3765,39.4702],[-0.3780,39.4716],[-0.3789,39.4735]]},
"legs":[{"steps":[
 {"distance":23,"duration":18,"name":"","maneuver":{"type":"depart","modifier":"left"}},
 {"distance":52,"duration":41,"name":"","maneuver":{"type":"turn","modifier":"left"}},
 {"distance":17,"duration":14,"name":"","maneuver":{"type":"turn","modifier":"right"}},
 {"distance":186,"duration":149,"name":"Plaça de l'Ajuntament","maneuver":{"type":"end of road","modifier":"left"}},
 {"distance":4,"duration":3,"name":"","maneuver":{"type":"turn","modifier":"left"}},
 {"distance":23,"duration":18,"name":"Plaça de l'Ajuntament","maneuver":{"type":"turn","modifier":"slight right"}},
 {"distance":90,"duration":72,"name":"Avinguda de Maria Cristina","maneuver":{"type":"end of road","modifier":"slight right"}},
 {"distance":8,"duration":6,"name":"Avinguda de Maria Cristina","maneuver":{"type":"continue","modifier":"slight right"}},
 {"distance":83,"duration":66,"name":"Plaça del Mercat","maneuver":{"type":"continue","modifier":"slight right"}},
 {"distance":0,"duration":0,"name":"Carrer de Palafox","maneuver":{"type":"arrive","modifier":"right"}}
],"distance":543.3,"duration":434.8}],"distance":543.3,"duration":434.8}],"waypoints":[]}
"""

class OsrmApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: OsrmApi
    private val from = LatLon(39.4699, -0.3763)
    private val to = LatLon(39.4735, -0.3789)

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        api = OsrmApi(OkHttpClient.Builder().callTimeout(2, TimeUnit.SECONDS).build(), server.url("/").toString().trimEnd('/'))
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
    }

    private fun failure(block: suspend () -> Unit): RouteException {
        try {
            runBlocking { block() }
        } catch (e: RouteException) {
            return e
        }
        fail("Debía lanzar RouteException")
        error("inalcanzable")
    }

    @Test
    fun `pide la ruta a pie con longitud y latitud y devuelve el plan`() {
        server.enqueue(MockResponse().setBody(FootJson))

        val plan = runBlocking { api.route(RouteProfile.Foot, from, to) }

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertTrue(request.path!!.startsWith("/routed-foot/route/v1/driving/-0.376300,39.469900;-0.378900,39.473500"))
        assertTrue(request.path!!.contains("geometries=geojson"))
        assertTrue(request.path!!.contains("steps=true"))
        assertTrue(request.getHeader("User-Agent")!!.startsWith("TurismoValencia/"))
        assertEquals(543, plan.meters)
        assertEquals(434, plan.seconds)
        assertEquals(8, plan.minutes)
        assertEquals(4, plan.points.size)
        assertEquals(LatLon(39.4699, -0.3763), plan.points.first())
        assertEquals(LatLon(39.4735, -0.3789), plan.points.last())
    }

    @Test
    fun `la ruta en bici usa su perfil`() {
        server.enqueue(MockResponse().setBody(FootJson))
        runBlocking { api.route(RouteProfile.Bike, from, to) }
        assertTrue(server.takeRequest().path!!.startsWith("/routed-bike/"))
    }

    @Test
    fun `los tramos cortos se suman al siguiente y quedan tres pasos mas la llegada`() {
        server.enqueue(MockResponse().setBody(FootJson))
        val steps = runBlocking { api.route(RouteProfile.Foot, from, to) }.steps
        assertEquals(
            listOf(
                RouteStep(StepKind.Left, null, 75), // «depart» de 23 m + giro de 52 m, sin nombre
                RouteStep(StepKind.Left, "Plaça de l'Ajuntament", 203), // 17 m + 186 m
                RouteStep(StepKind.Right, "Avinguda de Maria Cristina", 117), // 4 m + 23 m + 90 m
                RouteStep(StepKind.Arrive, null, null),
            ),
            steps,
        )
    }

    @Test
    fun `error del servidor`() {
        server.enqueue(MockResponse().setResponseCode(503))
        assertTrue(failure { api.route(RouteProfile.Foot, from, to) }.message!!.contains("503"))
    }

    @Test
    fun `respuesta sin ruta o que no es JSON`() {
        server.enqueue(MockResponse().setBody("""{"code":"NoRoute","routes":[]}"""))
        server.enqueue(MockResponse().setBody("no es json"))
        server.enqueue(MockResponse().setBody("""{"code":"Ok","routes":[{"geometry":{"coordinates":[[-0.37,39.47]]},"distance":1,"duration":1}]}"""))
        repeat(3) { failure { api.route(RouteProfile.Foot, from, to) } }
    }

    @Test
    fun `servidor que no responde o caido`() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        failure { api.route(RouteProfile.Foot, from, to) }
        server.shutdown()
        failure { api.route(RouteProfile.Foot, from, to) }
    }
}

class OsrmParsingTest {
    @Test
    fun `solo los giros de verdad cuentan como girar`() {
        assertEquals(StepKind.Straight, stepKind("depart", "left"))
        assertEquals(StepKind.Straight, stepKind("continue", "slight right"))
        assertEquals(StepKind.Straight, stepKind("new name", "straight"))
        assertEquals(StepKind.Left, stepKind("turn", "left"))
        assertEquals(StepKind.Left, stepKind("end of road", "slight left"))
        assertEquals(StepKind.Right, stepKind("turn", "sharp right"))
        assertEquals(StepKind.Straight, stepKind("turn", "straight"))
        assertEquals(StepKind.Straight, stepKind("turn", null))
        assertEquals(StepKind.Arrive, stepKind("arrive", "right"))
    }

    @Test
    fun `condensar deja la llegada aunque no haya pasos`() {
        assertEquals(listOf(RouteStep(StepKind.Arrive, null, null)), condense(emptyList()))
    }

    @Test
    fun `un ultimo paso corto no se pierde`() {
        val steps = listOf(OsrmStep(StepKind.Straight, "Calle A", 200), OsrmStep(StepKind.Left, "Calle B", 12))
        assertEquals(
            listOf(RouteStep(StepKind.Straight, "Calle A", 200), RouteStep(StepKind.Left, "Calle B", 12), RouteStep(StepKind.Arrive, null, null)),
            condense(steps),
        )
    }

    @Test
    fun `solo se enseñan tres pasos antes de la llegada`() {
        val steps = (1..6).map { OsrmStep(StepKind.Straight, "Calle $it", 100) }
        val shown = condense(steps)
        assertEquals(4, shown.size)
        assertEquals("Calle 3", shown[2].street)
        assertNull(shown.last().meters)
    }
}
