package com.cmarcillovargas.turismovalencia.ui.screens.reservas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cmarcillovargas.turismovalencia.R
import com.cmarcillovargas.turismovalencia.data.containerFactory
import com.cmarcillovargas.turismovalencia.data.fake.FakeOfertas
import com.cmarcillovargas.turismovalencia.data.model.Category
import com.cmarcillovargas.turismovalencia.data.model.Reservation
import com.cmarcillovargas.turismovalencia.data.model.ReservationStatus
import com.cmarcillovargas.turismovalencia.ui.components.PlaceImage
import com.cmarcillovargas.turismovalencia.ui.components.SectionLabel
import com.cmarcillovargas.turismovalencia.ui.components.SegmentedTabs
import com.cmarcillovargas.turismovalencia.ui.components.StatusChip
import com.cmarcillovargas.turismovalencia.ui.components.StatusTone
import com.cmarcillovargas.turismovalencia.ui.screens.favoritos.TabHeader
import com.cmarcillovargas.turismovalencia.ui.theme.Spacing
import com.cmarcillovargas.turismovalencia.ui.theme.TurismoValenciaTheme
import com.cmarcillovargas.turismovalencia.ui.theme.appColors
import com.cmarcillovargas.turismovalencia.util.formatEuro
import com.cmarcillovargas.turismovalencia.util.formatShortDate
import java.time.LocalDate

private val ThumbSize = 72.dp
private val ThumbShape = RoundedCornerShape(16.dp)
private val CardShape20 = RoundedCornerShape(20.dp)

@Composable
fun ReservasRoute(
    onOpenReservation: (String) -> Unit,
    onOpenProfile: () -> Unit,
    viewModel: ReservasViewModel = viewModel(factory = containerFactory { ReservasViewModel(it) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ReservasScreen(uiState, onOpenReservation, onOpenProfile, viewModel::onSelectTab)
}

@Composable
fun ReservasScreen(
    uiState: ReservasUiState,
    onOpenReservation: (String) -> Unit,
    onOpenProfile: () -> Unit,
    onSelectTab: (ReservationsTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TabHeader(
            title = stringResource(R.string.res_title),
            subtitle = "${pluralStringResource(R.plurals.res_count, uiState.total, uiState.total)} · ${pluralStringResource(R.plurals.res_pending, uiState.pending, uiState.pending)}",
            onOpenProfile = onOpenProfile,
        )
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            item {
                SegmentedTabs(
                    labels = listOf(R.string.res_tab_upcoming, R.string.res_tab_history).map { stringResource(it) },
                    selectedIndex = uiState.tab.ordinal,
                    onSelect = { onSelectTab(ReservationsTab.entries[it]) },
                )
            }
            section(R.string.res_this_week, uiState.thisWeek, onOpenReservation)
            section(R.string.res_later, uiState.later, onOpenReservation)
            section(R.string.res_completed, uiState.completed, onOpenReservation)
            if (uiState.thisWeek.isEmpty() && uiState.later.isEmpty() && uiState.completed.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.res_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(Spacing.l),
                    )
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(title: Int, rows: List<ReservationRow>, onOpen: (String) -> Unit) {
    if (rows.isEmpty()) return
    item { SectionLabel(stringResource(title)) }
    rows.forEach { row -> item(key = row.reservation.id) { ReservationCard(row, onOpen) } }
}

@Composable
private fun ReservationCard(row: ReservationRow, onOpen: (String) -> Unit) {
    val reservation = row.reservation
    Surface(
        modifier = Modifier.fillMaxWidth().clip(CardShape20).clickable(role = Role.Button) { onOpen(reservation.id) },
        shape = CardShape20,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = Spacing.xxs / 2,
    ) {
        Row(Modifier.padding(Spacing.s), verticalAlignment = Alignment.CenterVertically) {
            PlaceImage(row.offer?.let { it.thumb ?: it.image }, Category.Sostenible, Modifier.size(ThumbSize).clip(ThumbShape), reservation.title, assetPath = row.offer?.assetImage)
            Spacer(Modifier.width(Spacing.m))
            Column(Modifier.weight(1f)) {
                Text(reservation.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 2)
                IconLine(Icons.Outlined.CalendarMonth, "${formatShortDate(reservation.date)} · ${reservation.time}")
                IconLine(Icons.Outlined.Groups, stringResource(R.string.res_people_price, reservation.people, formatEuro(reservation.total)))
                StatusChip(
                    text = stringResource(reservation.status.label()),
                    tone = reservation.status.tone(),
                    icon = reservation.status.icon(),
                )
            }
            Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(Spacing.m))
        }
    }
}

@Composable
private fun IconLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.appColors.brand, modifier = Modifier.size(Spacing.l - Spacing.xxs))
        Spacer(Modifier.width(Spacing.xs))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

fun ReservationStatus.label(): Int = when (this) {
    ReservationStatus.Confirmed -> R.string.res_status_confirmed
    ReservationStatus.Pending -> R.string.res_status_pending
    ReservationStatus.Completed -> R.string.res_status_completed
}

fun ReservationStatus.tone(): StatusTone = when (this) {
    ReservationStatus.Confirmed -> StatusTone.Success
    ReservationStatus.Pending -> StatusTone.Warning
    ReservationStatus.Completed -> StatusTone.Neutral
}

fun ReservationStatus.icon() = when (this) {
    ReservationStatus.Confirmed -> Icons.Outlined.Check
    ReservationStatus.Pending -> Icons.Outlined.HourglassEmpty
    ReservationStatus.Completed -> Icons.Outlined.CheckCircle
}

private val PreviewRows = listOf(
    ReservationRow(Reservation("1", "albufera-barca", "Paseo en barca por la Albufera", "El Palmar", LocalDate.now().plusDays(3), "18:30", 2, 0, "x", 36, ReservationStatus.Confirmed, "TV-1"), FakeOfertas.first { it.id == "albufera-barca" }),
    ReservationRow(Reservation("2", "taller-paella", "Taller de paella valenciana", "Mercado", LocalDate.now().plusDays(10), "13:00", 4, 0, "x", 180, ReservationStatus.Pending, "TV-2"), FakeOfertas.first { it.id == "taller-paella" }),
    ReservationRow(Reservation("3", "ruta-ciutat-vella", "Ruta guiada Ciutat Vella", "Centro", LocalDate.now().minusDays(5), "10:00", 1, 0, "x", 15, ReservationStatus.Completed, "TV-3"), FakeOfertas.first { it.id == "ruta-ciutat-vella" }),
)

@Preview(name = "F1 · Mis reservas", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ReservasPreview() {
    TurismoValenciaTheme {
        ReservasScreen(ReservasUiState(total = 3, pending = 1, thisWeek = PreviewRows.take(1), later = PreviewRows.slice(1..1), completed = PreviewRows.takeLast(1)), {}, {}, {})
    }
}
