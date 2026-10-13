package com.cmarcillovargas.turismovalencia.data.model

/** Qué hay que hacer en un paso de la ruta. */
enum class StepKind { Straight, Left, Right, Arrive }

/** Paso de una ruta: la calle a la que se llega y los metros que se recorren (`null` en la llegada). */
data class RouteStep(val kind: StepKind, val street: String?, val meters: Int?)

/** Cómo se desplaza el usuario; con servicio de rutas real solo a pie y en bici. */
enum class RouteProfile { Foot, Bike }

/** Ruta calculada: el trazado, su distancia y duración, y sus primeros pasos. */
data class RoutePlan(val points: List<LatLon>, val meters: Int, val seconds: Int, val steps: List<RouteStep>) {
    val minutes: Int get() = (seconds + 59) / 60
}
