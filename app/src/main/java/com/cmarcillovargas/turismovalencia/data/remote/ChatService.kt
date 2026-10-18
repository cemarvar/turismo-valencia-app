package com.cmarcillovargas.turismovalencia.data.remote

import com.cmarcillovargas.turismovalencia.data.model.LanguageCode
import com.cmarcillovargas.turismovalencia.util.WebBaseUrl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Un turno de la conversación que viaja al servidor como contexto. */
data class ChatTurn(val fromUser: Boolean, val text: String)

/** No hay respuesta válida del asistente en línea (sin red, error del servidor o respuesta vacía). */
class ChatException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Asistente en línea de ValBot. */
interface ChatService {
    /** Devuelve la respuesta del asistente (en [language], el idioma de la app) o lanza [ChatException]. */
    suspend fun ask(message: String, history: List<ChatTurn>, language: LanguageCode = LanguageCode.ES): String
}

/**
 * Función `chat` de la web (`POST /api/chat`, Gemini detrás). La clave vive solo en el servidor: la app envía
 * `{ message, history: [{ role: "user" | "assistant", text }], lang }` y recibe `{ reply }` o `{ error }`. `lang` es la etiqueta
 * BCP 47 del idioma de la app (`es`, `ca-ES-valencia`, `en`, `fr`): la función de la web todavía no la lee (contesta en el idioma de
 * la pregunta) y es el dato que le hace falta para contestar siempre en el idioma elegido.
 */
class ChatApi(
    private val client: OkHttpClient = defaultClient(),
    private val url: String = "$WebBaseUrl/api/chat",
) : ChatService {

    override suspend fun ask(message: String, history: List<ChatTurn>, language: LanguageCode): String = withContext(Dispatchers.IO) {
        try {
            val response = client.newCall(request(message, history, language)).await()
            response.use { parse(it) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: ChatException) {
            throw e
        } catch (e: Exception) {
            // IOException (sin red, tiempo agotado) y cualquier otro fallo: la app responde con reglas.
            throw ChatException("No se pudo contactar con ValBot", e)
        }
    }

    private fun request(message: String, history: List<ChatTurn>, language: LanguageCode): Request {
        val turns = JSONArray()
        history.forEach { turns.put(JSONObject().put("role", if (it.fromUser) "user" else "assistant").put("text", it.text)) }
        val body = JSONObject().put("message", message).put("history", turns).put("lang", language.tag).toString()
        return Request.Builder().url(url).post(body.toRequestBody(JsonType)).build()
    }

    private fun parse(response: Response): String {
        val raw = response.body?.string().orEmpty()
        val json = try {
            JSONObject(raw)
        } catch (_: JSONException) {
            null
        }
        if (!response.isSuccessful) {
            val reason = json?.optString("error")?.takeIf { it.isNotBlank() } ?: "HTTP ${response.code}"
            throw ChatException("ValBot respondió ${response.code}: $reason")
        }
        val reply = json?.optString("reply")?.trim().orEmpty()
        if (reply.isEmpty()) throw ChatException("Respuesta vacía de ValBot")
        return reply
    }

    companion object {
        private val JsonType = "application/json; charset=utf-8".toMediaType()

        /** Cliente con los tiempos de ValBot; parte de [base] para compartir con el resto de la app el mismo grupo de conexiones. */
        fun defaultClient(base: OkHttpClient = OkHttpClient()): OkHttpClient = base.newBuilder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(40, TimeUnit.SECONDS)
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

/**
 * Solo para depuración (`--ez chatFake true`): responde como lo haría Gemini, con Markdown y nombres de fichas,
 * para ver cómo se pintan las respuestas en línea sin depender del servidor.
 */
class CannedChatService : ChatService {
    override suspend fun ask(message: String, history: List<ChatTurn>, language: LanguageCode): String {
        delay(900)
        return "¡Claro! Para empezar en Valencia te recomiendo:\n\n" +
            "* **Mercado Central**: uno de los mercados más bonitos de Europa, ideal para probar producto local.\n" +
            "* **Lonja de la Seda**: joya del gótico civil y Patrimonio de la Humanidad.\n\n" +
            "Si te apetece algo típico, prueba la *paella valenciana* y, de postre, horchata con fartons."
    }
}

/** Solo para depuración (`--ez chatDown true`): el asistente en línea no responde, así que ValBot contesta con las reglas de la app. */
class DownChatService : ChatService {
    override suspend fun ask(message: String, history: List<ChatTurn>, language: LanguageCode): String =
        throw ChatException("ValBot en línea desactivado (depuración)")
}
