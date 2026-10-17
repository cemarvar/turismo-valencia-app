package com.cmarcillovargas.turismovalencia.data.repo

import com.cmarcillovargas.turismovalencia.data.local.OfferEntity
import com.cmarcillovargas.turismovalencia.data.model.Offer
import com.cmarcillovargas.turismovalencia.data.model.OfferKind
import com.cmarcillovargas.turismovalencia.data.remote.OfferDto
import com.cmarcillovargas.turismovalencia.data.remote.TurismoApiClient
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

// Del JSON de la API a la caché de Room y de ahí al modelo de la app. Es Kotlin puro (sin Android) para probarlo en la JVM.

private val SlotsSerializer = ListSerializer(String.serializer())

/** `entrada`, `tour` o `experiencia` (la API); cualquier otro valor es una oferta que esta versión de la app no sabe pintar. */
internal fun offerKindOf(raw: String): OfferKind? = when (raw.trim().lowercase()) {
    "entrada" -> OfferKind.Entrada
    "tour" -> OfferKind.Tour
    "experiencia" -> OfferKind.Experiencia
    else -> null
}

/** La oferta lista para guardar, o `null` si no es válida (sin id o título, precio negativo o un tipo que la app no conoce). */
internal fun OfferDto.toEntity(position: Int): OfferEntity? {
    val offerKind = offerKindOf(kind) ?: return null
    if (id.isBlank() || title.isBlank() || priceFromCents < 0) return null
    return OfferEntity(
        id = id.trim(),
        position = position,
        kind = offerKind,
        tag = tag,
        title = title,
        duration = duration,
        place = place,
        extra = extra,
        priceFromCents = priceFromCents,
        description = description,
        placeId = placeId?.takeIf { it.isNotBlank() },
        slots = TurismoApiClient.Json.encodeToString(SlotsSerializer, slots),
    )
}

/** Los horarios guardados; si el texto no se puede leer, ninguno (nunca cierra la app por un dato de la caché). */
internal fun decodeSlots(json: String): List<String> =
    try {
        TurismoApiClient.Json.decodeFromString(SlotsSerializer, json)
    } catch (_: SerializationException) {
        emptyList()
    }

/**
 * La oferta como la ve la app. Los datos son los del servidor; la presentación (las fotos del diseño) sale de [bundled], el catálogo
 * incluido en la app, para las ofertas que ya conoce; la foto de la web ([assetImage]) se busca por id. Una oferta nueva del
 * servidor se pinta sin foto del diseño.
 */
internal fun OfferEntity.toModel(bundled: Offer?, assetImage: String?): Offer = Offer(
    id = id,
    kind = kind,
    tag = tag,
    title = title,
    duration = duration,
    place = place,
    extra = extra,
    priceFromCents = priceFromCents,
    image = bundled?.image,
    description = description,
    times = decodeSlots(slots),
    thumb = bundled?.thumb,
    ticket = bundled?.ticket,
    placeId = placeId,
    assetImage = assetImage,
)
