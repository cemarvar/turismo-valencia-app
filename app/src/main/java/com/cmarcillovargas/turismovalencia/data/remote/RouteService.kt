package com.cmarcillovargas.turismovalencia.data.remote

import com.cmarcillovargas.turismovalencia.data.model.LatLon
import com.cmarcillovargas.turismovalencia.data.model.RoutePlan
import com.cmarcillovargas.turismovalencia.data.model.RouteProfile
import com.cmarcillovargas.turismovalencia.data.model.RouteStep
import com.cmarcillovargas.turismovalencia.data.model.StepKind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** No se ha podido calcular la ruta (sin red, sin camino o respuesta no válida). */
class RouteException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Cálculo de rutas a pie y en bici. */
interface RouteService {
    /** Devuelve la ruta de [from] a [to] o lanza [RouteException]. */
    suspend fun route(profile: RouteProfile, from: LatLon, to: LatLon): RoutePlan
}

/**
 * Servidor OSRM de OpenStreetMap Alemania (FOSSGIS), con perfiles de calle para peatones y bicis, de uso libre con carga ligera.
 * Es un servicio comunitario sin garantías: si no responde, la app dibuja una ruta aproximada.
 */
class OsrmApi(
    private val client: OkHttpClient = defaultClient(),
    private val baseUrl: String = "https://routing.openstreetmap.de",
) : RouteService {

    override suspend fun route(profile: RouteProfile, from: LatLon, to: LatLon): RoutePlan = withContext(Dispatchers.IO) {
        try {
            val response = client.newCall(request(profile, from, to)).await()
            response.use {
                val body = it.body?.string().orEmpty()
                if (!it.isSuccessful) throw RouteException("El servicio de rutas respondió ${it.code}")
                parseOsrm(body)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: RouteException) {
            throw e
        } catch (e: Exception) {
            throw RouteException("No se pudo calcular la ruta", e)
        }
    }

    private fun request(profile: RouteProfile, from: LatLon, to: LatLon): Request {
        val segment = when (profile) {
            RouteProfile.Foot -> "routed-foot"
            RouteProfile.Bike -> "routed-bike"
        }
        // OSRM pide longitud,latitud; el perfil lo elige el servidor por la ruta (`routed-foot`), no por «driving».
        val coordinates = String.format(Locale.US, "%.6f,%.6f;%.6f,%.6f", from.lon, from.lat, to.lon, to.lat)
        return Request.Builder()
            .url("$baseUrl/$segment/route/v1/driving/$coordinates?overview=full&geometries=geojson&steps=true")
            .header("User-Agent", UserAgent)
            .build()
    }

    companion object {
        private const val UserAgent = "TurismoValencia/1.0 (Android; TFG DAM; https://github.com/cemarvar/turismo-valencia-app)"

        /** Cliente con tiempos cortos (quien espera la ruta es el usuario); parte de [base] para compartir conexiones. */
        fun defaultClient(base: OkHttpClient = OkHttpClient()): OkHttpClient = base.newBuilder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.SECONDS)
            .build()
    }
}

private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(
        object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (continuation.isActive) continuation.resume(response) else response.close()
            }
        },
    )
}

/** Máximo de pasos que se enseñan antes de la llegada; para el resto está la app de mapas («Iniciar»). */
private const val MaxSteps = 3

/** Los tramos más cortos que esto se suman al siguiente: la API trae muchos de pocos metros sin nombre. */
private const val ShortStepMeters = 30

/** Convierte la respuesta JSON de OSRM (`geometries=geojson`, `steps=true`) en una [RoutePlan]. */
internal fun parseOsrm(json: String): RoutePlan {
    val root = try {
        JSONObject(json)
    } catch (e: JSONException) {
        throw RouteException("Respuesta no válida del servicio de rutas", e)
    }
    if (root.optString("code") != "Ok") throw RouteException("Sin ruta: ${root.optString("code")}")
    val route = root.optJSONArray("routes")?.optJSONObject(0) ?: throw RouteException("Sin ruta")
    val coordinates = route.optJSONObject("geometry")?.optJSONArray("coordinates") ?: throw RouteException("Ruta sin trazado")
    val points = List(coordinates.length()) { index ->
        val pair = coordinates.getJSONArray(index)
        LatLon(pair.getDouble(1), pair.getDouble(0))
    }
    if (points.size < 2) throw RouteException("Ruta sin trazado")
    val steps = route.optJSONArray("legs")?.optJSONObject(0)?.optJSONArray("steps")?.let { array ->
        List(array.length()) { index ->
            val step = array.getJSONObject(index)
            val maneuver = step.optJSONObject("maneuver")
            OsrmStep(
                kind = stepKind(maneuver?.optString("type").orEmpty(), maneuver?.optString("modifier")),
                street = step.optString("name").takeIf { it.isNotBlank() },
                meters = step.optDouble("distance", 0.0).toInt(),
            )
        }
    }.orEmpty()
    return RoutePlan(
        points = points,
        meters = route.optDouble("distance", 0.0).toInt(),
        seconds = route.optDouble("duration", 0.0).toInt(),
        steps = condense(steps),
    )
}

internal data class OsrmStep(val kind: StepKind, val street: String?, val meters: Int)

/** Giros de OSRM: solo las maniobras que cambian de dirección cuentan como «girar»; `depart`, `continue`… son seguir recto. */
internal fun stepKind(type: String, modifier: String?): StepKind {
    if (type == "arrive") return StepKind.Arrive
    val turning = type in setOf("turn", "end of road", "fork", "on ramp", "off ramp", "roundabout", "rotary", "roundabout turn", "exit roundabout", "exit rotary")
    return when {
        !turning -> StepKind.Straight
        modifier?.contains("left") == true -> StepKind.Left
        modifier?.contains("right") == true -> StepKind.Right
        else -> StepKind.Straight
    }
}

/**
 * Deja una lista corta y legible: los tramos de menos de [ShortStepMeters] se suman al siguiente, se quedan los [MaxSteps]
 * primeros y se añade la llegada. Cada paso lleva la calle por la que se va y los metros hasta el siguiente.
 */
internal fun condense(steps: List<OsrmStep>): List<RouteStep> {
    val body = steps.filter { it.kind != StepKind.Arrive }
    val merged = mutableListOf<OsrmStep>()
    var carried = 0
    body.forEachIndexed { index, step ->
        val last = index == body.lastIndex
        if (step.meters < ShortStepMeters && !last) {
            carried += step.meters
        } else {
            merged += step.copy(meters = step.meters + carried)
            carried = 0
        }
    }
    val shown = merged.take(MaxSteps).map { RouteStep(it.kind, it.street, it.meters) }
    return shown + RouteStep(StepKind.Arrive, null, null)
}
