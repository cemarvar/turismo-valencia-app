package com.cmarcillovargas.turismovalencia.ui.screens.reservas

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.cmarcillovargas.turismovalencia.R
import com.cmarcillovargas.turismovalencia.data.repo.ReservationActionFailure

// Lo que se le cuenta al usuario cuando una reserva no sale. El texto depende del motivo (`Reason`), nunca del mensaje del servidor.

/** «Demasiados intentos seguidos», con los minutos de espera si el servidor los dio. */
@Composable
internal fun rateLimitedText(retryAfterSeconds: Int?): String =
    retryAfterSeconds?.let { stringResource(R.string.book_error_rate_limited_wait, ((it + 59) / 60).coerceAtLeast(1)) }
        ?: stringResource(R.string.book_error_rate_limited)

/** Por qué no se pudo cancelar la reserva. */
@Composable
internal fun ReservationActionFailure.cancelMessage(): String = when (reason) {
    // Puede que el servidor sí la cancelara y se perdiera la respuesta: se dice, y se invita a repetirlo (es seguro).
    ReservationActionFailure.Reason.Unreachable -> stringResource(R.string.booking_cancel_error_unreachable)
    ReservationActionFailure.Reason.Server -> stringResource(R.string.book_error_server)
    ReservationActionFailure.Reason.RateLimited -> rateLimitedText(retryAfterSeconds)
    ReservationActionFailure.Reason.NotFound -> stringResource(R.string.booking_cancel_error_not_found)
    ReservationActionFailure.Reason.TooLate -> stringResource(R.string.booking_cancel_error_too_late)
    ReservationActionFailure.Reason.Rejected -> stringResource(R.string.booking_cancel_error_other)
}

/** Por qué no se pudo recuperar la reserva. */
@Composable
internal fun ReservationActionFailure.recoverMessage(): String = when (reason) {
    ReservationActionFailure.Reason.Unreachable -> stringResource(R.string.book_error_unreachable)
    ReservationActionFailure.Reason.Server -> stringResource(R.string.book_error_server)
    ReservationActionFailure.Reason.RateLimited -> rateLimitedText(retryAfterSeconds)
    // El servidor no dice si falla el localizador o el correo, para que nadie pueda ir probando localizadores.
    ReservationActionFailure.Reason.NotFound -> stringResource(R.string.recover_error_not_found)
    ReservationActionFailure.Reason.TooLate, ReservationActionFailure.Reason.Rejected -> stringResource(R.string.recover_error_other)
}
