package com.cmarcillovargas.turismovalencia.ui.components

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.cmarcillovargas.turismovalencia.R

/**
 * Horario de una oferta que no es una hora: las entradas que valen todo el día llevan este texto, tal como lo manda el servidor (en
 * español). Es el valor que se guarda y se envía en la reserva; lo que se enseña es [slotLabel].
 */
const val AnyTimeSlot = "Cualquier hora"

/** El horario tal como se enseña, en el idioma de la app («18:30» se queda igual; «Cualquier hora» se traduce). */
@Composable
fun slotLabel(time: String): String = if (time == AnyTimeSlot) stringResource(R.string.book_any_time) else time

/** Lo mismo para quien no está en una pantalla (el texto de compartir una reserva). */
fun slotLabel(resources: Resources, time: String): String = if (time == AnyTimeSlot) resources.getString(R.string.book_any_time) else time
