package com.cmarcillovargas.turismovalencia.ui.screens.reservas

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cmarcillovargas.turismovalencia.R
import com.cmarcillovargas.turismovalencia.data.containerFactory
import com.cmarcillovargas.turismovalencia.data.fake.FakeOfertas
import com.cmarcillovargas.turismovalencia.data.model.Category
import com.cmarcillovargas.turismovalencia.data.model.Offer
import com.cmarcillovargas.turismovalencia.data.model.Reservation
import com.cmarcillovargas.turismovalencia.data.model.ReservationStatus
import com.cmarcillovargas.turismovalencia.ui.components.AppButton
import com.cmarcillovargas.turismovalencia.ui.components.AppButtonStyle
import com.cmarcillovargas.turismovalencia.ui.components.AppTopBar
import com.cmarcillovargas.turismovalencia.ui.components.BottomActionBar
import com.cmarcillovargas.turismovalencia.ui.components.PlaceImage
import com.cmarcillovargas.turismovalencia.ui.components.QrCode
import com.cmarcillovargas.turismovalencia.ui.components.StatusChip
import com.cmarcillovargas.turismovalencia.ui.theme.Spacing
import com.cmarcillovargas.turismovalencia.ui.theme.TurismoValenciaTheme
import com.cmarcillovargas.turismovalencia.ui.theme.appColors
import com.cmarcillovargas.turismovalencia.ui.theme.eyebrow
import com.cmarcillovargas.turismovalencia.util.formatEuro
import com.cmarcillovargas.turismovalencia.util.formatShortDate
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

private val IconCircle = 64.dp
private val TicketShape = RoundedCornerShape(20.dp)
private val TicketHeader = 110.dp
private val QrSize = 112.dp
private val QrShape = RoundedCornerShape(16.dp)

@Composable
fun ReservaConfirmadaRoute(
    id: String,
    onBack: () -> Unit,
    onOpenRoute: (String) -> Unit,
    onOpenMap: () -> Unit,
    viewModel: ReservaConfirmadaViewModel = viewModel(key = "reserva-$id", factory = containerFactory { ReservaConfirmadaViewModel(it, id) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    ReservaConfirmadaScreen(
        uiState = uiState,
        onBack = onBack,
        onShare = { uiState.reservation?.let { share(context, it) } },
        onCalendar = { uiState.reservation?.let { addToCalendar(context, it) } },
        onHowToGet = { uiState.offer?.placeId?.let(onOpenRoute) ?: onOpenMap() },
    )
}

@Composable
fun ReservaConfirmadaScreen(
    uiState: ReservaConfirmadaUiState,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onCalendar: () -> Unit,
    onHowToGet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reservation = uiState.reservation ?: return
    val offer = uiState.offer
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AppTopBar(stringResource(R.string.booking_title), onBack = onBack) {
            IconButton(onClick = onShare) {
                Icon(Icons.Outlined.Share, stringResource(R.string.common_share), tint = MaterialTheme.colorScheme.secondary)
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(Spacing.m),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val pending = reservation.status == ReservationStatus.Pending
            Box(
                Modifier.padding(top = Spacing.m).size(IconCircle).clip(CircleShape).background(if (pending) MaterialTheme.appColors.warningContainer else MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (pending) Icons.Outlined.HourglassEmpty else Icons.Outlined.Check,
                    contentDescription = null,
                    tint = if (pending) MaterialTheme.appColors.warning else MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(Spacing.xl + Spacing.xxs),
                )
            }
            Spacer(Modifier.height(Spacing.l))
            Text(
                stringResource(if (pending) R.string.booking_pending else R.string.booking_confirmed),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                stringResource(R.string.booking_sent_to, reservation.email),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.l))
            Ticket(reservation, offer)
        }
        BottomActionBar {
            AppButton(stringResource(R.string.detail_calendar), onCalendar, Modifier.weight(1f), AppButtonStyle.Outlined, leadingIcon = Icons.Outlined.CalendarMonth)
            AppButton(stringResource(R.string.detail_how_to_get), onHowToGet, Modifier.weight(1.2f), leadingIcon = Icons.Outlined.NearMe)
        }
    }
}

@Composable
private fun Ticket(reservation: Reservation, offer: Offer?) {
    Surface(Modifier.fillMaxWidth(), shape = TicketShape, color = MaterialTheme.colorScheme.surface, shadowElevation = Spacing.xxs / 2) {
        Column {
            Box(Modifier.fillMaxWidth().height(TicketHeader)) {
                PlaceImage(offer?.ticket ?: offer?.image, Category.Sostenible, Modifier.fillMaxSize(), reservation.title)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.2f to Color.Transparent, 1f to MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f))))
                Column(Modifier.align(Alignment.BottomStart).padding(Spacing.m)) {
                    Text("${offer?.tag.orEmpty()} · ${reservation.place}".uppercase(), style = MaterialTheme.typography.eyebrow, color = MaterialTheme.appColors.gold)
                    Text(reservation.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                }
            }
            Column(Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Row(Modifier.fillMaxWidth()) {
                    Detail(stringResource(R.string.booking_date), formatShortDate(reservation.date), Modifier.weight(1f))
                    Detail(stringResource(R.string.booking_time), reservation.time, Modifier.weight(1f))
                    Detail(stringResource(R.string.booking_people), peopleText(reservation), Modifier.weight(1.2f))
                }
                Row(Modifier.fillMaxWidth()) {
                    Detail(stringResource(R.string.booking_locator), reservation.locator, Modifier.weight(1.6f))
                    Detail(stringResource(R.string.booking_total), formatEuro(reservation.total), Modifier.weight(1f))
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.padding(Spacing.m), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(QrSize).clip(QrShape).border(1.dp, MaterialTheme.colorScheme.outlineVariant, QrShape).padding(Spacing.s)) {
                    QrCode(content = reservation.locator)
                }
                Spacer(Modifier.width(Spacing.m))
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    StatusChip(
                        text = stringResource(reservation.status.label()),
                        tone = reservation.status.tone(),
                        icon = reservation.status.icon(),
                        height = Spacing.xl,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(stringResource(R.string.booking_show_code), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun Detail(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun peopleText(reservation: Reservation): String = buildString {
    append(androidx.compose.ui.res.pluralStringResource(R.plurals.booking_adults, reservation.adults, reservation.adults))
    if (reservation.children > 0) {
        append(" · ")
        append(androidx.compose.ui.res.pluralStringResource(R.plurals.booking_children, reservation.children, reservation.children))
    }
}

private fun share(context: Context, reservation: Reservation) {
    val text = context.getString(R.string.booking_share_text, reservation.locator, reservation.title, formatShortDate(reservation.date), reservation.time)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.detail_share_chooser)))
}

private fun addToCalendar(context: Context, reservation: Reservation) {
    val time = runCatching { LocalTime.parse(reservation.time) }.getOrDefault(LocalTime.of(10, 0))
    val start = reservation.date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, reservation.title)
        putExtra(CalendarContract.Events.EVENT_LOCATION, reservation.place)
        putExtra(CalendarContract.Events.DESCRIPTION, reservation.locator)
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, start + 2 * 60 * 60 * 1000L)
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.detail_no_calendar, Toast.LENGTH_SHORT).show()
    }
}

@Preview(name = "F3 · Reserva confirmada", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ReservaConfirmadaPreview() {
    TurismoValenciaTheme {
        ReservaConfirmadaScreen(
            ReservaConfirmadaUiState(
                reservation = Reservation("r", "albufera-barca", "Paseo en barca por la Albufera", "El Palmar", LocalDate.now().plusDays(3), "18:30", 2, 0, "laura.garcia@email.com", 36, ReservationStatus.Confirmed, "TV-2026-0412"),
                offer = FakeOfertas.first { it.id == "albufera-barca" },
            ),
            {}, {}, {}, {},
        )
    }
}
