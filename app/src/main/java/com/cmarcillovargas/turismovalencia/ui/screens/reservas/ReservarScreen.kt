package com.cmarcillovargas.turismovalencia.ui.screens.reservas

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cmarcillovargas.turismovalencia.R
import com.cmarcillovargas.turismovalencia.data.containerFactory
import com.cmarcillovargas.turismovalencia.data.fake.FakeOfertas
import com.cmarcillovargas.turismovalencia.data.model.Category
import com.cmarcillovargas.turismovalencia.ui.components.AppButton
import com.cmarcillovargas.turismovalencia.ui.components.AppTopBar
import com.cmarcillovargas.turismovalencia.ui.components.BottomActionBar
import com.cmarcillovargas.turismovalencia.ui.components.Eyebrow
import com.cmarcillovargas.turismovalencia.ui.components.PlaceImage
import com.cmarcillovargas.turismovalencia.ui.theme.PillShape
import com.cmarcillovargas.turismovalencia.ui.theme.Spacing
import com.cmarcillovargas.turismovalencia.ui.theme.TurismoValenciaTheme
import com.cmarcillovargas.turismovalencia.ui.theme.appColors
import com.cmarcillovargas.turismovalencia.ui.theme.screenTitle
import com.cmarcillovargas.turismovalencia.util.formatEuro
import com.cmarcillovargas.turismovalencia.util.formatMonthYear
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val DayShape = RoundedCornerShape(16.dp)
private val SummaryShape = RoundedCornerShape(20.dp)
private val ThumbShape = RoundedCornerShape(16.dp)
private val CounterSize = 40.dp
private val FieldShape = RoundedCornerShape(16.dp)
private val SpanishLocale = Locale("es", "ES")

@Composable
fun ReservarRoute(
    offerId: String,
    onBack: () -> Unit,
    onConfirmed: (String) -> Unit,
    viewModel: ReservarViewModel = viewModel(key = "reservar-$offerId", factory = containerFactory { ReservarViewModel(it, offerId) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ReservarScreen(
        uiState = uiState,
        onBack = onBack,
        onSelectDate = viewModel::onSelectDate,
        onChangeWeek = viewModel::onChangeWeek,
        onSelectTime = viewModel::onSelectTime,
        onAdults = viewModel::onAdults,
        onChildren = viewModel::onChildren,
        onEmailChange = viewModel::onEmailChange,
        onConfirm = { viewModel.confirm(onConfirmed) },
    )
}

@Composable
fun ReservarScreen(
    uiState: ReservarUiState,
    onBack: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onChangeWeek: (Int) -> Unit,
    onSelectTime: (String) -> Unit,
    onAdults: (Int) -> Unit,
    onChildren: (Int) -> Unit,
    onEmailChange: (String) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val offer = uiState.offer ?: return
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AppTopBar(stringResource(R.string.book_title), subtitle = stringResource(R.string.book_step), onBack = onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            Surface(Modifier.fillMaxWidth(), shape = SummaryShape, color = MaterialTheme.colorScheme.surface, shadowElevation = Spacing.xxs / 2) {
                Row(Modifier.padding(Spacing.s), verticalAlignment = Alignment.CenterVertically) {
                    PlaceImage(offer.thumb ?: offer.image, Category.Sostenible, Modifier.size(Spacing.xxl + Spacing.l).clip(ThumbShape), offer.title, assetPath = offer.assetImage)
                    Spacer(Modifier.width(Spacing.s))
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                        Eyebrow(listOf(offer.tag, offer.place).filter { it.isNotBlank() }.joinToString(" · "))
                        Text(offer.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 2)
                        Text(
                            stringResource(R.string.book_per_person, offer.duration, formatEuro(offer.priceFrom)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            MonthHeader(uiState, onChangeWeek)
            WeekRow(uiState, onSelectDate)
            Text(stringResource(R.string.book_times), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                uiState.slots.forEach { slot ->
                    TimeChip(slot.time, slot.available, slot.time == uiState.time, { onSelectTime(slot.time) }, Modifier.weight(1f))
                }
            }
            CounterRow(stringResource(R.string.book_adults), stringResource(R.string.book_adults_price, formatEuro(offer.priceFrom)), uiState.adults, { onAdults(-1) }, { onAdults(1) }, minusEnabled = uiState.adults > 1)
            CounterRow(stringResource(R.string.book_children), stringResource(R.string.book_children_free), uiState.children, { onChildren(-1) }, { onChildren(1) }, minusEnabled = uiState.children > 0)
            EmailField(uiState.email, onEmailChange)
        }
        BottomActionBar {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.book_total_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatEuro(uiState.total), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            AppButton(stringResource(R.string.book_confirm), onConfirm, Modifier.weight(1.35f), trailingIcon = Icons.AutoMirrored.Outlined.ArrowForward, enabled = uiState.canConfirm)
        }
    }
}

@Composable
private fun MonthHeader(uiState: ReservarUiState, onChangeWeek: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            formatMonthYear(uiState.weekDays.getOrElse(3) { uiState.date }),
            style = MaterialTheme.typography.screenTitle,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        ArrowButton(Icons.Outlined.ChevronLeft, stringResource(R.string.book_prev_week), enabled = uiState.canGoBackWeek) { onChangeWeek(-1) }
        Spacer(Modifier.width(Spacing.xs))
        ArrowButton(Icons.Outlined.ChevronRight, stringResource(R.string.book_next_week), enabled = true) { onChangeWeek(1) }
    }
}

@Composable
private fun ArrowButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(Spacing.xxl - Spacing.xs)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxSize()) {
            Icon(icon, description, tint = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun WeekRow(uiState: ReservarUiState, onSelectDate: (LocalDate) -> Unit) {
    Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        uiState.weekDays.forEach { day ->
            val past = day.isBefore(uiState.today)
            val selected = day == uiState.date
            Column(
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (past) 0.45f else 1f)
                    .clip(DayShape)
                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                    .then(if (selected || past) Modifier else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, DayShape))
                    .selectable(selected = selected, enabled = !past, role = Role.Tab, onClick = { onSelectDate(day) })
                    .padding(vertical = Spacing.s),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val content = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                Text(
                    day.dayOfWeek.getDisplayName(TextStyle.NARROW, SpanishLocale).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) content else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(day.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = content)
            }
        }
    }
}

@Composable
private fun TimeChip(time: String, available: Boolean, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .height(Spacing.xl + Spacing.xs)
            .clip(PillShape)
            .background(
                when {
                    !available -> colors.surfaceContainerHighest
                    selected -> colors.primaryContainer
                    else -> colors.surface
                },
            )
            .border(
                BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.appColors.brand else if (available) colors.outlineVariant else Color.Transparent),
                PillShape,
            )
            .selectable(selected = selected, enabled = available, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = time,
            style = MaterialTheme.typography.labelLarge,
            color = when {
                !available -> colors.outline
                selected -> colors.onPrimaryContainer
                else -> colors.onSurface
            },
        )
    }
}

@Composable
private fun CounterRow(title: String, subtitle: String, value: Int, onMinus: () -> Unit, onPlus: () -> Unit, minusEnabled: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal), color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(
            Modifier.size(CounterSize).alpha(if (minusEnabled) 1f else 0.5f).border(1.dp, MaterialTheme.appColors.controlInactive, CircleShape).clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(onClick = onMinus, enabled = minusEnabled, modifier = Modifier.fillMaxSize()) {
                Icon(Icons.Outlined.Remove, stringResource(R.string.book_less), tint = MaterialTheme.colorScheme.secondary)
            }
        }
        Text(value.toString(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(Spacing.xxl), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Box(Modifier.size(CounterSize).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
            IconButton(onClick = onPlus, modifier = Modifier.fillMaxSize()) {
                Icon(Icons.Outlined.Add, stringResource(R.string.book_more), tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
private fun EmailField(email: String, onChange: (String) -> Unit) {
    val brand = MaterialTheme.appColors.brand
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FieldShape)
            .border(2.dp, brand, FieldShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = Spacing.m, vertical = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, tint = brand)
        Spacer(Modifier.width(Spacing.m))
        Column {
            Text(stringResource(R.string.book_email_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            BasicTextField(
                value = email,
                onValueChange = onChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(brand),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            )
        }
    }
}

@Preview(name = "F2 · Reservar", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ReservarPreview() {
    TurismoValenciaTheme {
        val today = LocalDate.now()
        val start = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        ReservarScreen(
            ReservarUiState(
                offer = FakeOfertas.first { it.id == "albufera-barca" },
                weekDays = (0..6).map { start.plusDays(it.toLong()) },
                today = today,
                date = today.plusDays(1),
                slots = listOf(TimeSlot("16:30", true), TimeSlot("17:30", true), TimeSlot("18:30", true), TimeSlot("19:30", false)),
                time = "18:30",
                email = "laura.garcia@email.com",
            ),
            {}, {}, {}, {}, {}, {}, {}, {},
        )
    }
}
