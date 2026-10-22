package com.cmarcillovargas.turismovalencia.data.repo

import org.json.JSONArray
import org.json.JSONObject

// Lectura tolerante de los JSON de `assets/` (org.json): lo que falta o está en blanco es `null` o una lista vacía, nunca una excepción.

/** El texto de [key], o `null` si falta, es `null` o está en blanco. */
internal fun JSONObject.optStringOrNull(key: String): String? = if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

internal fun JSONArray?.strings(): List<String> = if (this == null) emptyList() else List(length()) { optString(it) }

internal fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else List(length()) { getJSONObject(it) }

/** Filas «etiqueta · valor» (`[["a","b"],…]`). */
internal fun JSONArray?.pairs(): List<Pair<String, String>> =
    if (this == null) emptyList() else List(length()) { val row = getJSONArray(it); row.optString(0) to row.optString(1) }
